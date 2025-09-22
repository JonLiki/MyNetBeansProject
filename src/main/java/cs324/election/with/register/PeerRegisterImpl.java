/*
 * Enhanced Peer Register Implementation for LCR Leader Election
 */
package cs324.election.with.register;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class PeerRegisterImpl extends UnicastRemoteObject implements PeerRegister {

    private final int id = 0;
    public Registry registry;
    private ArrayList<Integer> peers = new ArrayList<>(); // Maintains registration order
    private boolean electionInProgress = false;
    private boolean recoveryMode = false; // FIXED: Track recovery elections

    // Ring building lock to prevent race conditions
    private final ReentrantLock ringLock = new ReentrantLock();

    // FIXED: Coordination lock for election starts/ends
    private final Object electionCoordination = new Object();

    /**
     * Constructs the PeerRegister.
     *
     * @throws RemoteException If export fails.
     */
    protected PeerRegisterImpl() throws RemoteException {
        // Initialize registry FIRST, before any other operations
        this.registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        peers.add(id);
        Logger.setLogLevel(Logger.LogLevel.WARN); // Default to WARN for simplified output
        printStartupBanner();
    }

    private void printStartupBanner() {
        System.out.println("\n" + Logger.separator(40));
        System.out.println("🏛️  PEER REGISTER READY");
        System.out.println("   Port: 1099 | ID: " + String.format("%03d", id));
        System.out.println(Logger.separator(40) + "\n");
    }

    @Override
    public synchronized void register(int id) throws RemoteException, AlreadyBoundException, NotBoundException {
        if (peers.contains(id)) {
            throw new RemoteException("PR: Node ID " + id + " already registered.");
        }
        if (electionInProgress) {
            throw new RemoteException("PR: Election in progress. Cannot register.");
        }

        peers.add(id);

        // Simple, clear registration message
        System.out.println("📝 Node " + String.format("%03d", id) + " REGISTERED");
        System.out.println("   Total nodes: " + (peers.size() - 1)); // Exclude PR (ID 0)

        if (peers.size() > 2) { // peers.size() includes PR (index 0)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            rebuildRing(); // Use full rebuild for 3+ nodes
        } else if (peers.size() == 2) { // First actual node (total 2 including PR)
            // Single node case - no ring needed yet
            System.out.println("📦 Node " + String.format("%03d", id) + " registered (waiting for 2nd node)");
        } else if (peers.size() == 3) { // Second actual node (total 3 including PR)
            // FIXED: Properly form 2-node ring
            int firstNodeId = peers.get(1);  // First registered node
            int secondNodeId = peers.get(2); // Current node

            try {
                Node firstNode = (Node) registry.lookup("Node" + firstNodeId);
                Node secondNode = (Node) registry.lookup("Node" + secondNodeId);

                // Create bidirectional ring: first ↔ second
                firstNode.setNextNode(secondNode);
                secondNode.setNextNode(firstNode);

                System.out.println("🔄 2-node ring formed: " + String.format("%03d", firstNodeId) + " ↔ " + String.format("%03d", secondNodeId));
            } catch (Exception e) {
                System.err.println("❌ Failed to form 2-node ring: " + e.getMessage());
            }
        }
    }

    /**
     * FIXED: Coordinated election start with lock
     */
    @Override
    public void beginElection() throws RemoteException {
        synchronized (electionCoordination) {
            if (!electionInProgress) {
                electionInProgress = true;
                recoveryMode = !isElectionCompleted(); // Track if this is recovery
                System.out.println("🚨 " + (recoveryMode ? "RECOVERY " : "") + "ELECTION STARTED - Registrations PAUSED");
            } else {
                System.out.println("⚠️ Election already in progress - ignoring duplicate start");
            }
        }
    }

    /**
     * FIXED: Coordinated election end with lock
     */
    @Override
    public void endElection() throws RemoteException {
        synchronized (electionCoordination) {
            if (electionInProgress) {
                electionInProgress = false;
                recoveryMode = false;
                System.out.println("✅ " + (recoveryMode ? "RECOVERY " : "") + "ELECTION ENDED - Registrations RESUMED");
            }
        }
    }

    @Override
    public int[] getRegisteredNodes() throws RemoteException {
        int[] nodeArray = new int[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            nodeArray[i] = peers.get(i);
        }
        return nodeArray;
    }

    /**
     * Rebuilds the ring topology - SORTED BY NODE ID for predictable message
     * flow
     */
    @Override
    public void rebuildRing() throws RemoteException {
        ringLock.lock();
        try {
            ArrayList<Node> activeNodes = new ArrayList<>();
            ArrayList<Integer> activeIds = new ArrayList<>();

            // Collect active nodes (skip PR id=0)
            for (int i = 1; i < peers.size(); i++) {
                int nodeId = peers.get(i);
                try {
                    Node node = (Node) registry.lookup("Node" + nodeId);
                    if (node.isAlive()) {
                        activeNodes.add(node);
                        activeIds.add(nodeId);
                    } else {
                        System.out.println("⚠️ Skipping inactive node " + nodeId);
                    }
                } catch (NotBoundException nbe) {
                    System.out.println("⚠️ Skipping unbound node " + nodeId + " (likely shutdown)");
                } catch (RemoteException re) {
                    System.out.println("⚠️ Skipping unreachable node " + nodeId);
                }
            }

            if (activeNodes.size() < 2) {
                System.out.println("⚠️  Cannot form ring - need 2+ nodes");
                return;
            }

            // SORT BY NODE ID for predictable topology
            Collections.sort(activeIds);

            // FIXED: Properly map sorted IDs to their corresponding Node objects
            ArrayList<Node> sortedActiveNodes = new ArrayList<>();
            for (int sortedId : activeIds) {
                // Find the Node object that matches this sorted ID
                for (Node node : activeNodes) {
                    try {
                        if (node.getId() == sortedId) {
                            sortedActiveNodes.add(node);
                            break; // Found match, move to next ID
                        }
                    } catch (RemoteException e) {
                        System.err.println("❌ Failed to get ID for node during sorting: " + e.getMessage());
                        break;
                    }
                }
            }

            // Verify we have all nodes (safety check)
            if (sortedActiveNodes.size() != activeIds.size()) {
                System.err.println("❌ RING REBUILD FAILED: Node-ID mismatch during sorting");
                return;
            }

            activeNodes = sortedActiveNodes;

            // ATOMIC RING BUILDING - Set connections in sorted order
            System.out.println("🔧 Rebuilding ring with nodes: " + activeIds.stream().map(n -> String.format("%03d", n)).collect(Collectors.joining(" → ")));

            for (int i = 0; i < activeNodes.size(); i++) {
                Node current = activeNodes.get(i);
                int currentId = activeIds.get(i);
                Node next = activeNodes.get((i + 1) % activeNodes.size());
                int nextId = activeIds.get((i + 1) % activeNodes.size());

                try {
                    // Set the connection: current -> next (circular)
                    current.setNextNode(next);
                    System.out.println("🔗 Connecting " + String.format("%03d", currentId) + " → " + String.format("%03d", nextId));

                    // Small delay to ensure RMI calls complete
                    Thread.sleep(100);
                } catch (RemoteException e) {
                    System.err.println("❌ Failed to connect " + currentId + " → " + nextId + ": " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Single, clear summary line
            String ringDesc = activeIds.stream().map(n -> String.format("%03d", n)).collect(Collectors.joining(" → ")) + " → " + String.format("%03d", activeIds.get(0));
            System.out.println("✅ RING BUILT: " + activeIds.size() + " nodes (" + ringDesc + ")");

        } catch (Exception e) {
            System.err.println("❌ RING REBUILD FAILED: " + e.getMessage());
        } finally {
            ringLock.unlock();
        }
    }

    // Node interface implementations (unchanged)
    @Override
    public int getId() throws RemoteException {
        return id; // PeerRegister has ID 0
    }

    @Override
    public String getStatus() throws RemoteException {
        return "PEER_REGISTER_ACTIVE";
    }

    @Override
    public void recover() throws RemoteException {
        // No-op for PeerRegister
    }

    @Override
    public void printDetailedStatus() throws RemoteException {
        System.out.println("\n" + Logger.separator(40));
        System.out.println("📊 PEER REGISTER STATUS");
        System.out.println("   ID: " + String.format("%03d", id));
        System.out.println("   Active Nodes: " + (peers.size() - 1));
        System.out.println("   Election Active: " + (electionInProgress ? "🔄 YES" : "⏸️ NO"));
        System.out.println("   Recovery Mode: " + (recoveryMode ? "🔄 YES" : "⏸️ NO"));
        System.out.println("   Registered: " + peers.subList(1, peers.size()).toString());
        System.out.println(Logger.separator(40) + "\n");
    }

    private List<Integer> getSortedActiveNodes() {
        ArrayList<Integer> activeNodes = new ArrayList<>();
        for (int i = 1; i < peers.size(); i++) {
            int nodeId = peers.get(i);
            try {
                Node node = (Node) registry.lookup("Node" + nodeId);
                if (node.isAlive()) {
                    activeNodes.add(nodeId);
                }
            } catch (Exception e) {
                // Skip unavailable nodes
            }
        }
        Collections.sort(activeNodes);
        return activeNodes;
    }

    private String getRingDescription() {
        List<Integer> sortedNodes = getSortedActiveNodes();
        if (sortedNodes.size() < 2) {
            return "No ring formed";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedNodes.size(); i++) {
            int currentId = sortedNodes.get(i);
            int nextId = sortedNodes.get((i + 1) % sortedNodes.size());
            sb.append(String.format("%03d", currentId)).append(" → ");
        }
        sb.append(String.format("%03d", sortedNodes.get(0))); // Complete the circle
        return sb.toString();
    }

    @Override
    public boolean isElectionInProgress() throws RemoteException {
        return electionInProgress;
    }

    @Override
    public boolean isElectionCompleted() throws RemoteException {
        return !electionInProgress;
    }

    @Override
    public void setAlive(boolean alive) throws RemoteException {
        // No-op - PeerRegister doesn't fail
    }

    @Override
    public void initiateElection() throws RemoteException {
        // No-op, elections started by nodes
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true; // PeerRegister is always alive
    }

    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        // No-op - PeerRegister is not part of the ring
    }

    @Override
    public void receiveElection(int candidateId, int originId) throws RemoteException {
        // No-op - PeerRegister doesn't participate in elections
    }

    @Override
    public void receiveLeader(int leaderId, int originId) throws RemoteException {
        // Log leader announcement
        System.out.println("👑 Leader " + leaderId + " elected (origin: " + originId + ")");
    }

    /**
     * Clean shutdown with proper resource cleanup
     */
    private void shutdown() {
        System.out.println("🛑 Shutting down Peer Register...");

        try {
            if (registry != null) {
                registry.unbind("Node0");
                System.out.println("✅ Unbound PeerRegister from RMI registry");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not unbind PeerRegister from RMI: " + e.getMessage());
        }

        System.out.println("👋 Peer Register shutdown complete");
    }

    public static void main(String[] args) throws RemoteException {
        PeerRegisterImpl peerRegister = new PeerRegisterImpl();

        try {
            peerRegister.registry.bind("Node0", peerRegister);
            System.out.println("✅ PeerRegister bound as Node0");
        } catch (AlreadyBoundException e) {
            peerRegister.registry.rebind("Node0", peerRegister);
            System.out.println("✅ PeerRegister rebound as Node0");
        }

        // Simple shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            peerRegister.shutdown();
        }));

        System.out.println("🏛️  PeerRegister READY - Waiting for nodes (Ctrl+C to exit)");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
