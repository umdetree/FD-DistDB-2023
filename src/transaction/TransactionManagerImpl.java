package transaction;

import java.rmi.*;

import static java.lang.Thread.sleep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;
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
    // for transaction ids persistence
    protected final static String SAVE_FILE_PATH = "data/tm_txList.log";


    static Registry _rmiRegistry = null;
    private AtomicInteger transactionId = new AtomicInteger(0);

    class TransactionData implements Serializable {
        public int xid;
        public HashSet<ResourceManager> rmList;
    }

    private HashMap<Integer,TransactionData> transactionDataMap;

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

    // transaction lists persistence
    public void storeState() {
        File txMapFile = new File(SAVE_FILE_PATH);
        txMapFile.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(txMapFile));
            oout.writeObject(transactionDataMap);
            oout.flush();
        } catch (Exception e) {
            System.err.println("TM error: failed to store tx state");
        } finally {
            try {
                if (oout != null)
                    oout.close();
            } catch (IOException e1) {
            }
        }
    }

    public HashMap<Integer, TransactionData> loadState() {
        File txMapFile = new File(SAVE_FILE_PATH);
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(txMapFile));
            System.out.println("loading state from disk");
            HashMap<Integer, TransactionData> state = (HashMap<Integer, TransactionData>) oin.readObject();
            System.out.println("state loaded from disk");
            return state;
        } catch (Exception e) {
            System.err.println("TM error: failed to load tx state");
            return null;
        } finally {
            try {
                if (oin != null)
                    oin.close();
            } catch (IOException e1) {
            }
        }
    }

    // I think WC should use this to get xid
    public int startTransaction() throws RemoteException{
        synchronized (transactionDataMap) {
            int xid = transactionId.getAndIncrement();
            if (transactionDataMap.get(xid) != null) {
                System.err.println("TM error: failed to start xid " + xid);
                return -1;
            }
            TransactionData txData = new TransactionData();
            txData.xid = xid;
            // init empty resource manager list(set) for transaction xid
            txData.rmList = new HashSet<ResourceManager>();
            System.out.println("Create xid " + xid);
            transactionDataMap.put(xid, txData);
            storeState();
            return xid;
        }
    }

    public boolean enlist(int xid, ResourceManager rm) throws RemoteException {
        synchronized (transactionDataMap) {
            TransactionData data = transactionDataMap.get(xid);
            if (data == null) {
                // Do not new empty Transaction here, new in Start instead.
                // If data == null, throws new RemoteException().
                // It is possible when an RM was not prepare and then reconnects.
                // Since if any RM is not prepared, all RMs should abort.
                // The unprepared RM may have not been aborted by TM due to net issue.
                // Such RM still has the xid that should be removed by TM.
                // So ignore such enlist, it is not valid.
                System.out.println("warning: unknown xid " + xid);
                return false;
            }
            data.rmList.add(rm);
            storeState();
            System.out.println("Enlist RM " + rm.getID() + " to xid " + xid);
            return true;
        }
    }

    public void commit(int xid) throws RemoteException, InvalidTransactionException {
        // try {
        //     sleep(5000);
        // } catch (Exception e) {}

        TransactionData data = null;
        synchronized (transactionDataMap) {
            data = transactionDataMap.get(xid);
        }
        if (data == null) {
            System.out.println("No such xid " + xid);
            throw new InvalidTransactionException(xid, "No such xid" + xid);
        }

        // prepare stage
        // if all RMs are prepared, commit all
        // else abort all
        synchronized (transactionDataMap) {
            try {
                for (ResourceManager rm : data.rmList) {
                    if (!rm.prepare(xid)) {
                        throw new InvalidTransactionException(xid, rm.getID());
                    }
                    System.out.println("prepared " + xid + " " + rm.getID());
                }
            } catch (Exception e) {
                // someone is not prepared, abort all
                System.out.println("not prepared " + e);
                for (ResourceManager rm : data.rmList) {
                    try {
                        System.out.println("aborting " + xid + " " + rm.getID());
                        rm.abort(xid);
                        System.out.println("aborted " + xid + " " + rm.getID());
                    } catch (Exception e1) {
                        System.out.println("abort err: " + e1);
                    }
                }
                System.out.println("hi");
                transactionDataMap.remove(xid);
                throw new InvalidTransactionException(xid, "not prepared");
            }
        }
        System.out.println("all prepared " + xid);

        // commit stage
        // at this stage, we should retry every failed commit
        synchronized (transactionDataMap) {
            for (ResourceManager rm : data.rmList) {
                boolean committed = false;
                while (!committed) {
                    try {
                        System.out.println("committing " + xid + " " + rm.getID());
                        rm.commit(xid);
                        System.out.println("committed " + xid + " " + rm.getID());
                        committed = true;
                    } catch (Exception e) {
                        // retry
                        try {
                            sleep(500);
                        } catch (Exception e1) {
                        }
                    }
                }
            }
            transactionDataMap.remove(xid);
            storeState();

        }
    }

    public TransactionManagerImpl() throws RemoteException {
        this.transactionDataMap = new HashMap<>();
        HashMap<Integer, TransactionData> state = loadState();
        if (state != null) {
            this.transactionDataMap = state;
            int maxKey = 0;

            for (int key : state.keySet()) {
                if (key > maxKey) {
                    maxKey = key;
                }
            }
            transactionId.set(maxKey + 1);
        }
    }

    public boolean dieNow()
            throws RemoteException {
        System.exit(1);
        return true; // We won't ever get here since we exited above;
                     // but we still need it to please the compiler.
    }

    public void setDieTime(String time) throws RemoteException{
        // TODO not finished
        System.out.println("Not finished TM die time set to " + time);

    }

    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        TransactionData data = transactionDataMap.get(xid);
        if (data == null) {
            System.out.println("No such xid " + xid);
            throw new InvalidTransactionException(xid, "No such xid " + xid);
        } else {
            for (ResourceManager rm : data.rmList) {
                System.out.println("aborting " + xid + " " + rm.getID());
                rm.abort(xid);
            }
            transactionDataMap.remove(xid);
            storeState();
        }
    }

}
