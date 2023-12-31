package transaction;

import java.rmi.*;

/**
 * Interface for the Transaction Manager of the Distributed Travel
 * Reservation System.
 * <p>
 * Unlike WorkflowController.java, you are supposed to make changes
 * to this file.
 */

public interface TransactionManager extends Remote {


    public boolean dieNow()
            throws RemoteException;

    public void ping() throws RemoteException;

    public boolean enlist(int xid, ResourceManager rm) throws RemoteException;
    public void abort(int xid) throws RemoteException, InvalidTransactionException;
    public void setDieTime(String time) throws RemoteException;
    public int startTransaction() throws RemoteException;
    public void commit(int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException;
    /** The RMI name a TransactionManager binds to. */
    public static final String RMIName = "TM";
}
