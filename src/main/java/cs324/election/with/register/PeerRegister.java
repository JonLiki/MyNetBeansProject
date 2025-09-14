/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cs324.election.with.register;

/**
 *
 * @author sione.likiliki
 */
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerRegister extends Node {

    /**
     * Registers a new node with the given ID.
     *
     * @param id The unique ID of the node to register.
     * @throws RemoteException If registration fails (e.g., election in
     * progress).
     * @throws AlreadyBoundException If the node is already bound.
     * @throws NotBoundException If lookup fails.
     */
    void register(int id) throws RemoteException, AlreadyBoundException, NotBoundException;

    /**
     * Signals the start of an election, preventing new registrations.
     *
     * @throws RemoteException If remote call fails.
     */
    void beginElection() throws RemoteException;

    /**
     * Signals the end of an election, allowing new registrations.
     *
     * @throws RemoteException If remote call fails.
     */
    void endElection() throws RemoteException;

    /**
     * Retrieves the list of registered node IDs.
     *
     * @return An array of registered node IDs.
     * @throws RemoteException If retrieval fails.
     */
    int[] getRegisteredNodes() throws RemoteException;
}
