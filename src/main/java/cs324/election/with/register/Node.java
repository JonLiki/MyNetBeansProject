package cs324.election.with.register;

/**
 * Remote interface for nodes in the LCR leader election protocol. Enhanced for
 * demo presentation with additional status and control methods.
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Node extends Remote {

    void receiveElection(int candidateId, int originId) throws RemoteException;

    void receiveLeader(int leaderId, int originId) throws RemoteException;

    int getId() throws RemoteException;

    void setNextNode(Node nextNode) throws RemoteException;

    String getStatus() throws RemoteException;

    void recover() throws RemoteException;

    void printDetailedStatus() throws RemoteException;

    boolean isElectionInProgress() throws RemoteException;

    boolean isElectionCompleted() throws RemoteException;

    void setAlive(boolean alive) throws RemoteException;

    void initiateElection() throws RemoteException;

    boolean isAlive() throws RemoteException;
}
