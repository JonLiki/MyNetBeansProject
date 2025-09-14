/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cs324.election.with.register;

/**
 *
 * @author sione.likiliki
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Node extends Remote {

    void receiveElection(int candidateId, int originId) throws RemoteException;

    void receiveLeader(int leaderId, int originId) throws RemoteException;

    void setNextNode(Node nextNode) throws RemoteException;
}
