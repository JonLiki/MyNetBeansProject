/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cs324.election.with.register;

/**
 *
 * @author sione.likiliki
 */
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Scanner;

public class NodeImpl extends UnicastRemoteObject implements Node {

    private final int id;
    private int leaderId;
    private Node nextNode;
    private PeerRegister peerRegister;

    private static final int DELAY_MS = 2000; // Configurable delay
    private static final int MAX_RETRIES = 3; // Maximum retry attempts
    private static final int RETRY_DELAY_MS = 1000; // Delay between retries

    /**
     * Constructs a new Node with the given ID.
     *
     * @param nodeId The unique ID of this node.
     * @throws RemoteException If export fails.
     */
    public NodeImpl(int nodeId) throws RemoteException {
        this.id = nodeId;
    }

    /**
     * Sets the next node in the ring.
     *
     * @param nextNode The successor node.
     * @throws RemoteException If remote call fails.
     */
    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        this.nextNode = nextNode;
    }

    /**
     * Receives an election message and forwards based on LCR rules.
     *
     * @param candidateId The candidate's ID.
     * @param originId The originator's ID.
     * @throws RemoteException If forwarding fails after retries.
     */
    @Override
    public void receiveElection(int candidateId, int originId) throws RemoteException {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (this.id == originId) {
            leaderId = candidateId;
            log("ELEC", id + ": Sending coordinator message.");
            peerRegister.endElection();
            retryRemoteCall(() -> nextNode.receiveLeader(leaderId, this.id), "send leader message");
            return;
        }

        try {
            if (this.id > candidateId) {
                log("ELEC", id + ": Receiving election message. Forwarding my ID.");
                retryRemoteCall(() -> nextNode.receiveElection(id, originId), "forward election with my ID");
            } else if (this.id < candidateId) {
                log("ELEC", id + ": Receiving election message. Forwarding message.");
                retryRemoteCall(() -> nextNode.receiveElection(candidateId, originId), "forward election message");
            }
        } catch (RemoteException e) {
            log("ERROR", id + ": Failed to forward election message after retries: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Receives the leader announcement and forwards it.
     *
     * @param leaderId The elected leader's ID.
     * @param originId The originator's ID.
     * @throws RemoteException If forwarding fails after retries.
     */
    @Override
    public void receiveLeader(int leaderId, int originId) throws RemoteException {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (this.id == originId) {
            log("ELEC", id + ": New leader is " + leaderId);
            log("ELEC", id + ": Election is complete.");
            return;
        }

        log("ELEC", id + ": New leader is " + leaderId);
        this.leaderId = leaderId;
        retryRemoteCall(() -> nextNode.receiveLeader(leaderId, originId), "forward leader message");
    }

    /**
     * Initiates an election upon detecting leader failure.
     */
    public void initiateElection() {
        log("ELEC", id + ": Detected Leader failure. Initiating election...");
        try {
            peerRegister.beginElection();
            retryRemoteCall(() -> nextNode.receiveElection(id, id), "initiate election");
        } catch (RemoteException e) {
            log("ERROR", id + ": Failed to initiate election after retries: " + e.getMessage());
        }
    }

    /**
     * Displays the current status (leader and registered nodes).
     *
     * @throws RemoteException If status retrieval fails.
     */
    private void displayStatus() throws RemoteException {
        // Simulate fetching node list (peerRegister would need a getPeers method)
        log("STATUS", id + ": Current leader is " + (leaderId == 0 ? "None" : leaderId));
        // Note: PeerRegisterImpl needs a method to return peers list
        log("STATUS", id + ": Registered nodes: [Not implemented, requires peer register update]");
    }

    // Method to handle user input
    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            log("USER", "Type 'start' to initiate the election, 'status' to check status, or 'exit' to quit:");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("start")) {
                initiateElection();
            } else if (command.equalsIgnoreCase("status")) {
                try {
                    displayStatus();
                } catch (RemoteException e) {
                    log("ERROR", id + ": Failed to retrieve status: " + e.getMessage());
                }
            } else if (command.equalsIgnoreCase("exit")) {
                log("USER", "Exiting...");
                System.exit(0);
            } else {
                log("USER", "Invalid command.");
            }
        }
    }

    // Utility method to log with timestamp and prefix
    private static void log(String prefix, String message) {
        String timestamp = new Date().toString();
        System.out.println("[" + timestamp + "] [" + prefix + "] " + message);
    }

    // Utility method to retry remote calls with exception handling
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
                log("WARN", id + ": Failed to " + action + ", retry " + attempts + "/" + MAX_RETRIES + ": " + e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Interrupted during retry delay", ie);
                }
            }
        }
    }

    // Functional interface for remote method execution
    @FunctionalInterface
    private interface RemoteRunnable {

        void run() throws RemoteException;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            log("ERROR", "Usage: java cs324.election.with.register.NodeImpl <nodeId>");
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
            log("REG", nodeId + ": Registered with PeerRegister.");

            node.handleUserInput();
        } catch (Exception e) {
            log("ERROR", "Error: " + e.getMessage());
        }
    }
}
