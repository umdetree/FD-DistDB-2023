package transaction;

import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * A toy client of the Distributed Travel Reservation System.
 * 
 */

public class Client {
    /*
    origin test : add flight, add room, query flight, reserve flight, commit
     */
    private static boolean test1(WorkflowController wc) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        int xid = wc.start();

        if (!wc.addFlight(xid, "347", 230, 999)) {
            System.err.println("Add flight failed");
        }
        if (!wc.addRooms(xid, "SFO", 500, 150)) {
            System.err.println("Add room failed");
        }

        System.out.println("Flight 347 has " +
                wc.queryFlight(xid, "347") +
                " seats.");
        if (!wc.reserveFlight(xid, "John", "347")) {
            System.err.println("Reserve flight failed");
        }
        System.out.println("Flight 347 now has " +
                wc.queryFlight(xid, "347") +
                " seats.");

        if (!wc.commit(xid)) {
            System.err.println("Commit failed");
        }
        return true;
    }
    /*
    db initial test : add flight, add room, add car, commit
    */
    private static boolean test_initial(WorkflowController wc) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        int xid = wc.start();

        if (!wc.addFlight(xid, "996", 230, 125)) {
            System.err.println("Add flight failed");
        }
        if (!wc.addRooms(xid, "Shanghai", 500, 150)) {
            System.err.println("Add room failed");
        }

        if (!wc.addCars(xid, "Shanghai", 500, 150)) {
            System.err.println("Add car failed");
        }

        if (!wc.commit(xid)) {
            System.err.println("Commit failed");
        }
        return true;
    }
    /*
    db clear : del flight, del room, del car, commit
    */
    private static boolean test_clear(WorkflowController wc) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        int xid = wc.start();

        if (!wc.deleteFlight(xid, "996")) {
            System.err.println("Delete flight failed");
        } else {
            System.out.println("Delete flight success");
        }
        if (!wc.deleteRooms(xid, "Shanghai",500)) {
            System.err.println("Delete room failed");
        } else {
            System.out.println("Delete room success");
        }

        if (!wc.deleteCars(xid, "Shanghai",500)) {
            System.err.println("Delete car failed");
        } else {
            System.out.println("Delete car success");
        }

        if (!wc.commit(xid)) {
            System.err.println("Commit failed");
        }
        return true;
    }

    /*
    reservation test : add customer, reserve flight,reserve room,reserve car, query customer bill and assert, commit
    */
    private static boolean reservationTest(WorkflowController wc,String user) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        int xid = wc.start();
        int bill = -1;
        if (!wc.newCustomer(xid, user)) {
            System.err.println("Add customer failed");
        }
        if (!wc.reserveFlight(xid, user, "996")) {
            System.err.println("Reserve flight failed");
        }
        if (!wc.reserveRoom(xid, user, "Shanghai")) {
            System.err.println("Reserve room failed");
        }
        if (!wc.reserveCar(xid, user, "Shanghai")) {
            System.err.println("Reserve car failed");
        }
        bill = wc.queryCustomerBill(xid, user);
        if (bill != 125 + 150 + 150) {
            System.err.println("Customer's bill wrong " + bill);
        }

        if (!wc.commit(xid)) {
            System.err.println("Commit failed");
        } else {
            System.out.println("Reservation success");
        }
        return true;
    }
    /*
    unreservation test : delete customer, query customer bill and assert, commit
    */
    private static boolean unreservationTest(WorkflowController wc, String user) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        int xid = wc.start();
        int bill = -1;
        if (!wc.deleteCustomer(xid, user)) {
            System.err.println("Delete customer failed");
        }
        bill = wc.queryCustomerBill(xid, user);
        if (bill != -1) {
            System.err.println("Customer's bill wrong: " + bill);
        }
        if (!wc.commit(xid)) {
            System.err.println("Commit failed");
        }
        return true;
    }
    /*
    Combine test : initial, reservation, unreservation, clear
     */
    private static boolean CombineTestforReservation(WorkflowController wc) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        if (!test_initial(wc)) {
            System.err.println("Initial failed");
        } else {
            System.out.println("Initial success");
        }
        if (!reservationTest(wc,"Jason")) {
            System.err.println("Reservation failed");
        } else {
            System.out.println("Reservation success");
        }
        if (!unreservationTest(wc,"Jason")) {
            System.err.println("Unreservation failed");
        } else {
            System.out.println("Unreservation success");
        }
        if (!test_clear(wc)) {
            System.err.println("Clear failed");
        } else {
            System.out.println("Clear success");
        }
        return true;

    }
    /*
    open test: open other server

     */
    private static void openTest() throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        new Thread(new Runnable() {

            public void run() {
                TransactionManagerImpl.main(null);
            }
        }).start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {

            public void run() {
                RMManagerFlights.main(null);
            }
        }).start();
        new Thread(new Runnable() {

            public void run() {
                RMManagerCars.main(null);
            }
        }).start();
        new Thread(new Runnable() {

            public void run() {
                RMManagerCustomers.main(null);
            }
        }).start();
        new Thread(new Runnable() {

            public void run() {
                RMManagerHotels.main(null);
            }
        }).start();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {

            public void run() {
                WorkflowControllerImpl.main(null);
            }
        }).start();

    }
    public static void main(String args[]) {
        try {
            openTest();
        } catch (InvalidTransactionException | RemoteException | TransactionAbortedException e) {
            throw new RuntimeException(e);
        }


        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }
        String rmiPort = prop.getProperty("wc.port");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }

        WorkflowController wc = null;
        try {
            wc = (WorkflowController) Naming.lookup(rmiPort + WorkflowController.RMIName);
            System.out.println("Bound to WC");
        } catch (Exception e) {
            System.err.println("Cannot bind to WC:" + e);
            System.exit(1);
        }

        try {
            CombineTestforReservation(wc);

        } catch (Exception e) {
            System.err.println("Received exception:" + e);
            System.exit(1);
        }

    }
}
