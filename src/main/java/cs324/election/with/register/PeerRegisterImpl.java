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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;

public class PeerRegisterImpl extends UnicastRemoteObject implements PeerRegister {

    private final int id = 0;
    public Registry registry;
    private ArrayList<Integer> peers = new ArrayList<>();
    private boolean electionInProgress = false;

    /**
     * Constructs the PeerRegister.
     *
     * @throws RemoteException If export fails.
     */
    protected PeerRegisterImpl() throws RemoteException {
        peers.add(id);
        this.registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    }

    /**
     * Registers a new node and updates the ring topology among real nodes.
     *
     * @param id The ID to register.
     * @throws RemoteException If election in progress or lookup fails.
     * @throws AlreadyBoundException Not used here.
     * @throws NotBoundException If node lookup fails.
     */
    @Override
    public synchronized void register(int id) throws RemoteException, AlreadyBoundException, NotBoundException {
        log("REG", "Attempting to register Node " + id);
        if (peers.contains(id)) {
            throw new RemoteException("PR: Node ID " + id + " already registered.");
        }
        if (electionInProgress) {
            throw new RemoteException("PR: Election in progress. Cannot register.");
        }
        peers.add(id);
        log("REG", "Registered nodes: " + peers);

        if (peers.size() > 1) {
            Node newNode = (Node) registry.lookup("Node" + id);

            if (peers.size() == 2) {
                newNode.setNextNode(newNode);
            } else {
                int previousNodeID = peers.get(peers.size() - 2);
                Node previousNode = (Node) registry.lookup("Node" + previousNodeID);
                previousNode.setNextNode(newNode);

                int firstRealId = peers.get(1);
                Node firstNode = (Node) registry.lookup("Node" + firstRealId);
                newNode.setNextNode(firstNode);
            }
        }
        log("REG", "PR: Node " + id + " registered.");
    }

    @Override
    public void beginElection() throws RemoteException {
        electionInProgress = true;
        log("ELEC", "PR: Election started. Registrations paused.");
    }

    @Override
    public void endElection() throws RemoteException {
        electionInProgress = false;
        log("ELEC", "PR: Election ended. Registrations resumed.");
    }

    @Override
    public int[] getRegisteredNodes() throws RemoteException {
        int[] nodeArray = new int[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            nodeArray[i] = peers.get(i);
        }
        return nodeArray;
    }

    @Override
    public void setNextNode(Node nextNode) throws RemoteException {
        // No-op
    }

    @Override
    public void receiveElection(int candidateId, int originId) throws RemoteException {
        // No-op
    }

    @Override
    public void receiveLeader(int leaderId, int originId) throws RemoteException {
        // No-op
    }

    // Utility method to log with timestamp and prefix
    private static void log(String prefix, String message) {
        String timestamp = new Date().toString();
        System.out.println("[" + timestamp + "] [" + prefix + "] " + message);
    }

    public static void main(String[] args) throws RemoteException {
        PeerRegisterImpl peerRegister = new PeerRegisterImpl();

        try {
            peerRegister.registry.bind("Node0", peerRegister);
        } catch (AlreadyBoundException e) {
            log("ERROR", "Peer register is already bound.");
        }
        log("REG", "PR: Peer register node running...");
    }
}
