/*
 * LCR Leader Election Implementation with Register
 */
package cs324.election.with.register;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the Node interface for LCR leader election with central
 * register. Enhanced with demo features: visuals, failure simulation,
 * interactive CLI, etc.
 * 
 * @author sione.likiliki
 */
public class NodeImpl extends UnicastRemoteObject implements Node {

    // Core node properties
    private final int id;
    private volatile int leaderId = -1;
    private volatile Node nextNode;
    private volatile boolean isLeader = false;
    private volatile boolean isAlive = true;
    private PeerRegister peerRegister;

    // Election state management
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private final AtomicBoolean electionCompleted = new AtomicBoolean(false);
    private final AtomicBoolean leaderAnnounced = new AtomicBoolean(false);
    private final AtomicInteger electionRound = new AtomicInteger(0);
    private final ReentrantLock electionLock = new ReentrantLock();

    // Recovery coordination
    private final AtomicBoolean recoveryInitiated = new AtomicBoolean(false);
    private static final Object recoveryCoordination = new Object();

    // Async forwarding to prevent deadlocks
    private final ExecutorService asyncForwarder = Executors.newCachedThreadPool();

    // Timeout management
    private ScheduledExecutorService timeoutScheduler;
    private static final int MAX_ELECTION_ROUNDS = 5;

    // Demo configuration
    private static final long NETWORK_DELAY = 500;
    private static final int MAX_RETRIES = 15;
    private static final long RETRY_DELAY = 1500;
    private ScheduledExecutorService heartbeatScheduler;
    private static final long HEARTBEAT_INTERVAL = 5000;
    private static final long ELECTION_TIMEOUT = 60000;

    // Status tracking
    private final Object statusLock = new Object();
    private volatile String status = "INITIALIZING";

    /**
     * Constructs a new Node with the given ID.
     *
     * @param nodeId The unique ID of this node.
     * @throws RemoteException If export fails.
     */
    public NodeImpl(int nodeId) throws RemoteException {
        this.id = nodeId;
        Logger.setLogLevel(Logger.LogLevel.WARN);
        updateStatus("READY");
        printStartupMessage();
        startHeartbeatMonitor();
    }

    /**
     * Coordinated heartbeat monitoring with recovery coordination
     */
    private void startHeartbeatMonitor() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (leaderId != -1 && !isLeader && isAlive && electionCompleted.get()) {
                try {
                    Node leaderNode = (Node) LocateRegistry.getRegistry(Registry.REGISTRY_PORT).lookup("Node" + leaderId);
                    leaderNode.getStatus(); // Simple ping
                } catch (Exception e) {
                    System.out.println("💥 Node " + id + ": Leader " + leaderId + " FAILED! Preparing re-election.");

                    // Coordinate recovery to prevent multiple initiators
                    synchronized (recoveryCoordination) {
                        if (recoveryInitiated.compareAndSet(false, true)) {
                            // This node coordinates the recovery
                            System.out.println("👥 Node " + id + ": Coordinating recovery election");

                            // Reset local election state for recovery
                            resetElectionState();
                            leaderId = -1;
                            isLeader = false;

                            // Rebuild ring to exclude failed leader
                            try {
                                peerRegister.rebuildRing();
                                System.out.println("🔧 Node " + id + ": Ring rebuilt after leader failure");
                            } catch (RemoteException re) {
                                System.err.println("❌ Node " + id + ": Failed to rebuild ring: " + re.getMessage());
                            }

                            // Start recovery election
                            try {
                                initiateRecoveryElection();
                            } catch (RemoteException re) {
                                System.err.println("❌ Node " + id + ": Failed to initiate recovery: " + re.getMessage());
                                recoveryInitiated.set(false); // Allow retry
                            }
                        } else {
                            // Other node is already coordinating - just reset local state
                            System.out.println("👥 Node " + id + ": Recovery coordinated by another node");
                            resetElectionState();
                            leaderId = -1;
                            isLeader = false;
                        }
                    }
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Dedicated recovery election that coordinates with other nodes
     */
    private void initiateRecoveryElection() throws RemoteException {
        // Always allow recovery election (bypass normal checks)
        if (nextNode == null) {
            System.err.println("❌ Node " + id + ": Cannot initiate recovery election - no ring connection");
            recoveryInitiated.set(false); // Allow retry
            return;
        }

        electionInProgress.set(true);
        try {
            peerRegister.beginElection();
        } catch (RemoteException e) {
            System.err.println("❌ Node " + id + ": Failed to notify PeerRegister of recovery election start");
        }

        int currentRound = electionRound.incrementAndGet();
        System.out.println("🔄 Node " + id + ": Initiating RECOVERY election (Round " + currentRound + ")");

        // Rebuild ring before starting recovery
        try {
            peerRegister.rebuildRing();
            System.out.println("🔧 Node " + id + ": Pre-recovery ring rebuild complete");
        } catch (RemoteException e) {
            System.err.println("❌ Node " + id + ": Failed pre-recovery ring rebuild: " + e.getMessage());
        }

        // Start recovery election with asynchronous forwarding
        asyncForwarder.submit(() -> {
            try {
                Thread.sleep(NETWORK_DELAY);
                // Use special recovery originator ID to distinguish from normal elections
                nextNode.receiveElection(id, id);
            } catch (InterruptedException e) {
                System.err.println("❌ Node " + id + ": Interrupted during recovery election initiation");
                Thread.currentThread().interrupt();
            } catch (RemoteException e) {
                System.err.println("❌ Node " + id + ": Failed to initiate recovery election message");
                recoveryInitiated.set(false); // Allow retry by another node
            }
        });

        // Schedule recovery timeout
        timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutScheduler.schedule(() -> {
            if (electionInProgress.get() && !electionCompleted.get()) {
                System.out.println("⚠️ Node " + id + ": Recovery election timeout in round " + currentRound + " - retrying");
                resetElectionState();
                recoveryInitiated.set(false); // Allow another node to try
                if (electionRound.get() < MAX_ELECTION_ROUNDS) {
                    try {
                        initiateRecoveryElection();
                    } catch (RemoteException e) {
                        System.err.println("❌ Node " + id + ": Failed to retry recovery election");
                    }
                }
            }
        }, ELECTION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Update node status with thread safety
     */
    private void updateStatus(String newStatus) {
        synchronized (statusLock) {
            status = newStatus;
        }
    }

    /**
     * Connection change detection to reduce duplicate logging
     */
    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        // Detect if connection actually changed (compare Node objects)
        boolean connectionChanged = false;
        if (this.nextNode == null && nextNode != null) {
            connectionChanged = true;
        } else if (this.nextNode != null && nextNode != null) {
            try {
                // Only log if the next node ID actually changed
                int oldNextId = this.nextNode.getId();
                int newNextId = nextNode.getId();
                connectionChanged = (oldNextId != newNextId);
            } catch (RemoteException e) {
                // If we can't get IDs, assume it changed to be safe
                connectionChanged = true;
            }
        } else if (this.nextNode != null && nextNode == null) {
            connectionChanged = true;
        }

        this.nextNode = nextNode;

        if (connectionChanged) {
            updateStatus("CONNECTED");
            System.out.println("🔗 Node " + id + ": Connected to ring");

            // Log current connection only if we can get the next node ID
            try {
                if (nextNode != null) {
                    System.out.println("🔗 Node " + id + ": Connection: " + id + " → " + nextNode.getId());
                } else {
                    System.out.println("⚠️ Node " + id + ": Connection: " + id + " → DISCONNECTED");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Node " + id + ": Could not determine next node connection");
            }
        }
        // Silently update if connection didn't change
    }

    @Override
    public void setAlive(boolean alive) throws RemoteException {
        if (isAlive != alive) {
            isAlive = alive;
            if (alive) {
                updateStatus("RECOVERED");
                System.out.println("🔥 Node " + id + ": Recovered from failure");
                recover(); // Trigger recovery
            } else {
                updateStatus("FAILED");
                System.out.println("💀 Node " + id + ": Node failed");
            }
        }
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return isAlive;
    }

    @Override
    public void recover() throws RemoteException {
        if (!isAlive) {
            setAlive(true);
        }
        // Re-register or rebuild ring via PR
        try {
            peerRegister.rebuildRing();
            System.out.println("🔧 Node " + id + ": Ring repaired after recovery");
        } catch (Exception e) {
            System.err.println("❌ Node " + id + ": Recovery failed: " + e.getMessage());
        }
    }

    /**
     * LCR ELECTION HANDLING
     */
    @Override
    public void receiveElection(int candidateId, int originId) throws RemoteException {
        if (!isAlive || electionCompleted.get()) {
            System.out.println("⚠️ Node " + id + ": SKIPPING election message - node dead or already completed");
            return;
        }

        updateStatus("ELECTION_MSG(" + candidateId + ")");
        System.out.println("📨 Node " + id + ": Received ELECTION(" + candidateId + ", " + originId + ")");

        // Simulate network delay
        try {
            Thread.sleep(NETWORK_DELAY);
        } catch (InterruptedException e) {
            System.err.println("❌ Node " + id + ": Interrupted during network delay");
            Thread.currentThread().interrupt();
        }

        electionLock.lock();
        try {
            if (nextNode == null) {
                System.out.println("❌ Node " + id + ": No next node available - DROPPING election message");
                return;
            }

            // Handle both higher candidate AND originator replacement
            if (candidateId > id || (candidateId == id && originId != id)) {
                // Either forwarding higher ID OR claiming originator status
                if (candidateId == id && originId != id) {
                    System.out.println("👑 Node " + id + ": CLAIMING ORIGINATOR STATUS: Replacing ELECTION(" + candidateId + ", " + originId + ") with ELECTION(" + id + ", " + id + ")");
                    electionInProgress.set(true);
                    forwardElectionAsync(id, id); // Use SELF as new originator
                } else {
                    System.out.println("🔄 Node " + id + " → Forwarding ELECTION(" + candidateId + ")");
                    electionInProgress.set(true);
                    forwardElectionAsync(candidateId, originId);
                }
            } else if (candidateId < id && !electionInProgress.get()) {
                System.out.println("🔄 Node " + id + " → Starting ELECTION(" + id + ")");
                electionInProgress.set(true);
                forwardElectionAsync(id, originId);
            } else if (candidateId == id && originId == id) {
                // CIRCUIT COMPLETION: Only originator recognizes its own ID returning
                System.out.println("🎉 Node " + id + " → CIRCUIT COMPLETE! I AM LEADER!");
                announceLeader(candidateId);
            } else {
                System.out.println("🗑️  Node " + id + ": DISCARDING election message - already participating or stale origin");
            }
        } finally {
            electionLock.unlock();
        }
    }

    @Override
    public void receiveLeader(int leaderId, int originId) throws RemoteException {
        if (!isAlive || leaderAnnounced.get()) {
            System.out.println("⚠️ Node " + id + ": SKIPPING leader message - node dead or leader already announced");
            return;
        }

        this.leaderId = leaderId;
        this.isLeader = (leaderId == id);
        leaderAnnounced.set(true);
        electionInProgress.set(false);
        electionCompleted.set(true);
        updateStatus("LEADER_ANNOUNCED(" + leaderId + ")");
        System.out.println("👑 Node " + id + ": Accepts Leader " + leaderId);

        // Forward if not self (stops circulation after full circuit)
        if (leaderId != id && nextNode != null) {
            System.out.println("🔄 Node " + id + ": Forwarding leader announcement to next node");
            forwardLeaderAsync(leaderId, originId);
        }
    }

    /**
     * Block normal elections during recovery coordination
     */
    @Override
    public void initiateElection() throws RemoteException {
        // Block normal elections during recovery coordination
        if (electionInProgress.get() || recoveryInitiated.get() || (electionCompleted.get() && leaderId != -1)) {
            System.out.println("⚠️ Node " + id + ": Cannot start normal election - recovery in progress or valid leader exists");
            return;
        }

        if (nextNode == null) {
            System.err.println("❌ Node " + id + ": Cannot initiate election - no ring connection established");
            return;
        }

        electionInProgress.set(true);
        try {
            peerRegister.beginElection();
        } catch (RemoteException e) {
            System.err.println("❌ Node " + id + ": Failed to notify PeerRegister of election start");
        }

        int currentRound = electionRound.incrementAndGet();
        System.out.println("🚀 Node " + id + ": Initiating election (Round " + currentRound + ")");

        // Rebuild ring before starting to ensure valid topology
        try {
            peerRegister.rebuildRing();
            System.out.println("🔧 Node " + id + ": Pre-election ring rebuild complete");
        } catch (RemoteException e) {
            System.err.println("❌ Node " + id + ": Failed pre-election ring rebuild: " + e.getMessage());
        }

        // Start election with asynchronous forwarding
        asyncForwarder.submit(() -> {
            try {
                Thread.sleep(NETWORK_DELAY);
                nextNode.receiveElection(id, id); // Start with own ID as originator
            } catch (InterruptedException e) {
                System.err.println("❌ Node " + id + ": Interrupted during election initiation");
                Thread.currentThread().interrupt();
            } catch (RemoteException e) {
                System.err.println("❌ Node " + id + ": Failed to initiate election message");
            }
        });

        // Schedule timeout
        timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutScheduler.schedule(() -> {
            if (electionInProgress.get() && !electionCompleted.get()) {
                System.out.println("⚠️ Node " + id + ": Election timeout in round " + currentRound + " - retrying");
                resetElectionState();
                if (electionRound.get() < MAX_ELECTION_ROUNDS) {
                    try {
                        initiateElection();
                    } catch (RemoteException e) {
                        System.err.println("❌ Node " + id + ": Failed to retry election");
                    }
                } else {
                    System.err.println("💥 Node " + id + ": ELECTION FAILURE - Maximum retry attempts exceeded");
                }
            }
        }, ELECTION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * ASYNCHRONOUS election message forwarding
     */
    private void forwardElectionAsync(int candidateId, int originId) {
        if (nextNode == null) {
            System.out.println("⚠️ Node " + id + ": Cannot forward election - no next node");
            return;
        }

        asyncForwarder.submit(() -> {
            try {
                Thread.sleep(NETWORK_DELAY);
                System.out.println("🔄 Node " + id + " → ELECTION(" + candidateId + ")");

                retryRemoteCall(() -> nextNode.receiveElection(candidateId, originId), "forward election message");

            } catch (InterruptedException e) {
                System.err.println("❌ Node " + id + ": Interrupted while forwarding election message");
                Thread.currentThread().interrupt();
            } catch (RemoteException e) {
                System.err.println("❌ Node " + id + ": RMI Error forwarding election message");
                // Try to rebuild ring on forwarding failure
                try {
                    peerRegister.rebuildRing();
                    System.out.println("⚠️ Node " + id + ": Attempted ring rebuild after forwarding failure");
                } catch (Exception rebuildEx) {
                    System.err.println("❌ Node " + id + ": Failed to rebuild ring: " + rebuildEx.getMessage());
                }
            }
        });
    }

    /**
     * Reset recovery coordination on successful election
     */
    private void announceLeader(int leaderId) {
        this.leaderId = leaderId;
        this.isLeader = (leaderId == id);
        leaderAnnounced.set(true);
        electionInProgress.set(false);
        electionCompleted.set(true);

        // Reset recovery coordination on successful election
        recoveryInitiated.set(false);

        updateStatus("LEADER_ANNOUNCED(" + leaderId + ")");

        if (isLeader) {
            System.out.println("🏆 Node " + id + ": Declares itself LEADER!");
        } else {
            System.out.println("🏆 Node " + id + ": Leader " + leaderId + " elected");
        }

        try {
            peerRegister.endElection();
            System.out.println("✅ Node " + id + ": Notified PeerRegister: Election completed");
        } catch (RemoteException e) {
            System.err.println("❌ Node " + id + ": Failed to notify PeerRegister of election completion");
        }

        // Start leader announcement with asynchronous forwarding
        if (nextNode != null) {
            System.out.println("👑 Node " + id + ": Starting leader announcement");
            forwardLeaderAsync(leaderId, id);
        }
    }

    /**
     * ASYNCHRONOUS leader announcement forwarding
     */
    private void forwardLeaderAsync(int leaderId, int originId) {
        if (nextNode == null) {
            System.out.println("⚠️ Node " + id + ": Cannot forward leader announcement - no next node");
            return;
        }

        asyncForwarder.submit(() -> {
            try {
                Thread.sleep(NETWORK_DELAY / 2);
                System.out.println("👑 Node " + id + " → LEADER(" + leaderId + ")");
                retryRemoteCall(() -> nextNode.receiveLeader(leaderId, originId), "forward leader message");
            } catch (InterruptedException e) {
                System.err.println("❌ Node " + id + ": Interrupted while forwarding leader announcement");
                Thread.currentThread().interrupt();
            } catch (RemoteException e) {
                System.err.println("❌ Node " + id + ": RMI Error forwarding leader announcement");
            }
        });
    }

    /**
     * Enhanced reset for full recovery
     */
    private void resetElectionState() {
        electionInProgress.set(false);
        leaderAnnounced.set(false);
        electionCompleted.set(false);
        // Also reset leader flags for recovery
        isLeader = false;
        // Do not reset leaderId here - handler sets to -1
        if (timeoutScheduler != null && !timeoutScheduler.isShutdown()) {
            timeoutScheduler.shutdownNow();
        }
        System.out.println("🔄 Node " + id + ": Election state fully reset for recovery");
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public String getStatus() throws RemoteException {
        synchronized (statusLock) {
            return status + " | Leader: " + (leaderId == -1 ? "None" : leaderId)
                    + " | Alive: " + isAlive
                    + " | Election: " + (electionInProgress.get() ? "IN_PROGRESS" : electionCompleted.get() ? "COMPLETED" : "IDLE");
        }
    }

    @Override
    public void printDetailedStatus() throws RemoteException {
        Logger.printStatusHeader("📊 Node " + String.format("%03d", id) + " Status");
        Logger.info("Node ID: " + String.format("%03d", id));
        Logger.info("Status: " + status);
        Logger.info("Leader: " + (leaderId == -1 ? "None" : String.format("%03d", leaderId)));
        Logger.info("Is Leader: " + (isLeader ? "👑 YES" : "NO"));
        Logger.info("Alive: " + (isAlive ? "🟢 YES" : "🔴 NO"));
        Logger.info("Election: " + (electionInProgress.get() ? "🔄 IN_PROGRESS" : electionCompleted.get() ? "✅ COMPLETED" : "⏸️ IDLE"));
        Logger.info("Round: " + electionRound.get());
        Logger.info("Next Node: " + (nextNode != null ? "✅ CONNECTED" : "❌ DISCONNECTED"));
        try {
            Logger.info("Next Node ID: " + (nextNode != null ? String.format("%03d", nextNode.getId()) : "N/A"));
        } catch (Exception e) {
            Logger.info("Next Node ID: ERROR");
        }
        Logger.printStatusFooter();
    }

    @Override
    public boolean isElectionInProgress() throws RemoteException {
        return electionInProgress.get();
    }

    @Override
    public boolean isElectionCompleted() throws RemoteException {
        return electionCompleted.get();
    }

    private void printStartupMessage() {
        System.out.println("\n" + Logger.separator(30));
        System.out.println("🚀 Node " + String.format("%03d", id) + " STARTED");
        System.out.println("   Waiting for registration...");
        System.out.println(Logger.separator(30));
        System.out.println("💡 Type 'help' for commands");
    }

    private void printHelpBanner() {
        System.out.println("\n" + Logger.separator(35));
        System.out.println("📖 Node " + String.format("%03d", id) + " COMMANDS");
        System.out.println("   start    - 🚀 Start election");
        System.out.println("   kill     - 💀 Simulate failure");
        System.out.println("   recover  - 🔥 Recover node");
        System.out.println("   leader   - 👑 Show leader");
        System.out.println("   status   - 📊 Node status");
        System.out.println("   debug    - 🔍 Detailed info");
        System.out.println("   help     - 📖 This help");
        System.out.println("   exit     - 👋 Shutdown");
        System.out.println(Logger.separator(35) + "\n");
    }

    // Simplified retry method - logging handled by caller
    private void retryRemoteCall(RemoteRunnable runnable, String action) throws RemoteException {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                runnable.run();
                return;
            } catch (RemoteException e) {
                attempts++;
                if (attempts == MAX_RETRIES) {
                    throw e;
                }
                System.out.println("⚠️ Node " + id + ": Failed to " + action + ", retry " + attempts + "/" + MAX_RETRIES);
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Interrupted during retry", ie);
                }
            }
        }
    }

    @FunctionalInterface
    private interface RemoteRunnable {

        void run() throws RemoteException;
    }

    /**
     * Enhanced shutdown with explicit alive state
     */
    private void shutdown() {
        System.out.println("🛑 Node " + id + ": Shutting down node...");

        // Explicitly set alive false before unbind
        isAlive = false;

        // Shutdown executors
        if (asyncForwarder != null && !asyncForwarder.isShutdown()) {
            asyncForwarder.shutdown();
        }
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
        }
        if (timeoutScheduler != null && !timeoutScheduler.isShutdown()) {
            timeoutScheduler.shutdown();
        }

        // Unbind from RMI registry
        try {
            Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            registry.unbind("Node" + id);
            System.out.println("✅ Node " + id + ": Unbound from RMI registry");
        } catch (Exception e) {
            System.out.println("⚠️ Node " + id + ": Could not unbind from RMI: " + e.getMessage());
        }

        System.out.println("👋 Node " + id + ": Node shutdown complete");
    }

    public void handleUserInput() {
        Scanner scanner = new Scanner(System.in);

        // Register shutdown hook for clean exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }));

        while (true) {
            try {
                System.out.print("Node " + String.format("%03d", id) + " > ");
                System.out.flush();

                String command = scanner.nextLine().trim().toLowerCase();

                switch (command) {
                    case "start":
                        initiateElection();
                        break;
                    case "kill":
                        setAlive(false);
                        break;
                    case "recover":
                        recover();
                        break;
                    case "leader":
                        System.out.println("👑 Node " + id + ": Current leader: " + (leaderId == -1 ? "None" : leaderId));
                        break;
                    case "debug":
                        printDetailedStatus();
                        break;
                    case "status":
                        System.out.println("📊 Node " + String.format("%03d", id) + " Status");
                        System.out.println("   State: " + status);
                        System.out.println("   Leader: " + (leaderId == -1 ? "None" : leaderId));
                        System.out.println("   Alive: " + (isAlive ? "🟢 YES" : "🔴 NO"));
                        System.out.println("   Election: " + (electionInProgress.get() ? "🔄 ACTIVE" : electionCompleted.get() ? "✅ DONE" : "⏸️ IDLE"));
                        break;
                    case "reset":
                        resetElectionState();
                        System.out.println("🔄 Node " + id + ": Election state reset");
                        break;
                    case "help":
                        printHelpBanner();
                        break;
                    case "exit":
                        shutdown();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("⚠️ Node " + id + ": Unknown command: " + command + ". Type 'help' for commands.");
                }

                // Flush after command processing to ensure log messages appear before next prompt
                System.out.flush();

            } catch (Exception e) {
                System.err.println("❌ Node " + id + ": Command processing error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("❌ Usage: java cs324.election.with.register.NodeImpl <nodeId>");
            return;
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            NodeImpl node = new NodeImpl(nodeId);

            Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            node.peerRegister = (PeerRegister) registry.lookup("Node0");

            try {
                registry.bind("Node" + nodeId, node);
            } catch (AlreadyBoundException e) {
                registry.rebind("Node" + nodeId, node);
            }

            node.peerRegister.register(nodeId);
            System.out.println("✅ Node " + nodeId + ": Registered with PeerRegister");

            node.handleUserInput();
        } catch (Exception e) {
            System.err.println("❌ Fatal Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
