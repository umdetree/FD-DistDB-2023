package transaction;

import java.rmi.*;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.atomic.*;

/**
 * Transaction Manager for the Distributed Travel Reservation System.
 * 
 * Description: toy implementation of the TM
 */

public class TransactionManagerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements TransactionManager {
    // for transaction ids persistance
    protected final static String SAVE_FILE_PATH = "data/tm_txList.log";

    static Registry _rmiRegistry = null;
    private AtomicInteger transactionId = new AtomicInteger(0);

    class TransationData {
        public int xid;
        public HashSet<ResourceManager> rmList;
    }

    private HashMap<Integer,TransationData> transationDataMap;

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }

        String rmiPort = prop.getProperty("tm.port");
        System.out.println(rmiPort);
        try {
            _rmiRegistry = LocateRegistry.createRegistry(Integer
                    .parseInt(rmiPort));
        } catch (RemoteException e2) {
            e2.printStackTrace();
            return;
        }

        String rmiName = TransactionManager.RMIName;
        if (rmiName == null || rmiName.equals("")) {
            System.err.println("No RMI name given");
            System.exit(1);
        }

        try {
            TransactionManagerImpl obj = new TransactionManagerImpl();
            _rmiRegistry.bind(rmiName, obj);

            System.out.println(rmiName + " bound");
        } catch (Exception e) {
            System.err.println(rmiName + " not bound:" + e);
            System.exit(1);
        }
    }

    public void ping() throws RemoteException {
    }

    public int Start() {
        synchronized (transationDataMap) {
            int xid = transactionId.getAndIncrement();
            if (transationDataMap.get(xid) != null) {
                System.err.println("TM error: failed to start xid " + xid);
                return -1;
            }
            TransationData txData = new TransationData();
            // init empty resource manager list(set) for transaction xid
            transationDataMap.put(xid, txData);
            return xid;
        }
    }

    public void enlist(int xid, ResourceManager rm) throws RemoteException {
        TransationData data = transationDataMap.get(xid);
        if (data == null) {
            data = new TransationData();
            data.xid = xid;
            data.rmList = new HashSet<ResourceManager>();
            System.out.println("Create xid " + xid);
            transationDataMap.put(xid, data);
        }
        data.rmList.add(rm);
        System.out.println("Enlist RM " + rm.getID() + " to xid " + xid);
    }

    public void commit(int xid) throws RemoteException, InvalidTransactionException {
        TransationData data = transationDataMap.get(xid);
        if (data == null) {
            System.out.println("No such xid " + xid);
            return;
        } else {
            for (ResourceManager rm : data.rmList) {
                System.out.println("committing " + xid + " " + rm.getID());
                rm.commit(xid);
            }
            transationDataMap.remove(xid);
        }
    }

    public TransactionManagerImpl() throws RemoteException {
        this.transationDataMap = new HashMap<>();
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
                     // but we still need it to please the compiler.
    }

}
