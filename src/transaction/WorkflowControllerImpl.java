package transaction;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Properties;

import lockmgr.DeadlockException;
import transaction.data.*;

import static java.lang.Thread.sleep;

/**
 * Workflow Controller for the Distributed Travel Reservation System.
 *
 * Description: toy implementation of the WC. In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */

public class WorkflowControllerImpl
        extends java.rmi.server.UnicastRemoteObject
        implements WorkflowController {
    static public String FLIGHTS = "FLIGHTS", HOTELS = "HOTELS", CARS ="CARS", CUSTOMERS ="CUSTOMERS",
            RESERVATIONS = "RESERVATIONS";

    protected int flightcounter, flightprice, carscounter, carsprice, roomscounter, roomsprice;
    protected int xidCounter;

    protected ResourceManager rmFlights = null;
    protected ResourceManager rmRooms = null;
    protected ResourceManager rmCars = null;
    protected ResourceManager rmCustomers = null;
    protected TransactionManager tm = null;

    static Registry _rmiRegistry = null;

    public static void main(String args[]) {
        System.setSecurityManager(new RMISecurityManager());

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return;
        }

        String rmiPort = prop.getProperty("wc.port");

        try {
            _rmiRegistry = LocateRegistry.createRegistry(Integer
                    .parseInt(rmiPort));
        } catch (RemoteException e2) {
            e2.printStackTrace();
            return;
        }

        String rmiName = WorkflowController.RMIName;
        if (rmiName == null || rmiName.equals("")) {
            System.err.println("No RMI name given");
            System.exit(1);
        }

        try {
            WorkflowControllerImpl obj = new WorkflowControllerImpl();
            _rmiRegistry.bind(rmiName, obj);
            System.out.println(rmiName + " bound");
        } catch (Exception e) {
            System.err.println(rmiName + " not bound:" + e);
            System.exit(1);
        }
    }

    public WorkflowControllerImpl() throws RemoteException, InterruptedException {
        flightcounter = 0;
        carscounter = 0;
        carsprice = 0;
        roomscounter = 0;
        roomsprice = 0;
        flightprice = 0;

        xidCounter = 1;

        while (!reconnect()) {
            sleep(1000);
            // would be better to sleep a while
        }

        new Thread() {
            public void run() {
                while (true) {
                    pingServices();

                    if (!isConnected()) {
                        try {
                            reconnect();
                            System.out.println("reconnect wc!");
                        } catch (Exception e) {
                            System.err.println("wc reconnect error: " + e);
                        }

                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }

                }
            }
        }.start();
    }

    private boolean isConnected() {
        boolean res = rmCars != null && rmRooms != null && rmFlights != null && rmCustomers != null && tm != null;
        return res;
    }

    private void pingServices() {
        try {
            tm.ping();
        } catch (Exception e) {
            System.out.println("cannnot ping tm");
            tm = null;
        }
        try {
            rmCars.ping();
        } catch (Exception e) {
            System.out.println("cannnot ping rmCars");
            rmCars = null;
        }
        try {
            rmCustomers.ping();
        } catch (Exception e) {
            System.out.println("cannnot ping rmCustomers");
            rmCustomers = null;
        }
        try {
            rmFlights.ping();
        } catch (Exception e) {
            System.out.println("cannnot ping rmFlights");
            rmFlights = null;
        }
        try {
            rmRooms.ping();
        } catch (Exception e) {
            System.out.println("cannnot ping rmRooms");
            rmRooms = null;
        }
    }

    // TRANSACTION INTERFACE
    public int start()
            throws RemoteException {
        int xid = tm.startTransaction();
        System.out.println("xid " + xid + " started");
        return xid;
    }

    public boolean commit(int xid)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        System.out.println("xid " + xid + " committing");
        tm.commit(xid);
        System.out.println("xid " + xid + " commit success");
        return true;
    }

    public void abort(int xid)
            throws RemoteException,
            InvalidTransactionException {
        return;
    }

    // ADMINISTRATIVE INTERFACE

    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        Flight flight = new Flight(flightNum,price,numSeats,numSeats);
        if (!flight.isVaild()){
            return false;
        }
        try {
            boolean success = rmFlights.insert(xid,FLIGHTS,flight);
            if (!success) {
                Flight flight_o = (Flight) rmFlights.query(xid,FLIGHTS,flightNum);
                if (flight_o == null){
                    return false;
                }
                flight_o.setNumSeats(flight_o.getNumSeats() + numSeats);
                flight_o.setNumAvail(flight_o.getNumAvail() + numSeats);
                flight_o.setPrice(price);
                System.out.println("update flight " + flightNum + " to " + flight_o.getNumAvail());
                return rmFlights.update(xid,FLIGHTS,flightNum,flight_o);
            }
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
        return true;
    }

    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Collection resveration = rmCustomers.query(xid,RESERVATIONS,"resvKey",flightNum);
            if (resveration != null && !resveration.isEmpty()) {
                System.out.println("resveration size " + resveration.size());
                return false;
            }

            return rmFlights.delete(xid,FLIGHTS,flightNum);
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        } catch (InvalidIndexException e) {
            throw new InvalidTransactionException(xid,e.getMessage());
        }

    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        Hotel hotel = new Hotel(location,price,numRooms,numRooms);
        if (!hotel.isVaild()){
            return false;
        }
        try {
            boolean success = rmRooms.insert(xid,HOTELS,hotel);
            if (!success) {
                Hotel hotel_o = (Hotel) rmRooms.query(xid,HOTELS,location);
                if (hotel_o == null){
                    return false;
                }
                hotel_o.setNumRooms(hotel_o.getNumRooms() + numRooms);
                hotel_o.setNumAvail(hotel_o.getNumAvail() + numRooms);
                hotel_o.setPrice(price);

                System.out.println("update hotel " + location + " to " + hotel_o.getNumAvail());
                return rmRooms.update(xid,HOTELS,location,hotel_o);
            }
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
        return true;
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,location);
            if (hotel == null) {
                return false;
            }
            hotel.setNumRooms(hotel.getNumRooms() - numRooms);
            hotel.setNumAvail(hotel.getNumAvail() - numRooms);
            if (!hotel.isVaild()) {
                return false;
            }
            return rmRooms.update(xid,HOTELS,location,hotel);
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        Car car = new Car(location,price,numCars,numCars);
        if (!car.isVaild()){
            return false;
        }
        try {
            boolean success = rmCars.insert(xid,CARS,car);
            if (!success) {
                Car car_o = (Car) rmCars.query(xid,CARS,location);
                if (car_o == null){
                    return false;
                }
                car_o.setNumCars(car_o.getNumCars() + numCars);
                car_o.setNumAvail(car_o.getNumAvail() + numCars);
                car_o.setPrice(price);
                System.out.println("update car " + location + " to " + car_o.getNumAvail());
                return rmCars.update(xid,CARS,location,car_o);
            }
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
        return true;
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Car car = (Car) rmCars.query(xid,CARS,location);
            if (car == null) {
                return false;
            }
            car.setNumCars(car.getNumCars() - numCars);
            car.setNumAvail(car.getNumAvail() - numCars);
            if (!car.isVaild()) {
                return false;
            }
            return rmCars.update(xid,CARS,location,car);
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        Customer customer = new Customer(custName);
        try {
            rmCustomers.insert(xid,CUSTOMERS,customer);
            return true;
        }  catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Collection resveration = rmCustomers.query(xid,RESERVATIONS,"custName",custName);
            if (resveration != null && !resveration.isEmpty()) {
                // reservation all reservations of this customer
                for (Object i : resveration) {
                    Reservation reservation = (Reservation) i;
                    ReservationKey key = new ReservationKey(reservation.getCustName(),reservation.getResvType(),reservation.getResvKey());
                    if (reservation.getResvType() == Reservation.RESERVATION_TYPE_FLIGHT) {
                        Flight flight = (Flight) rmFlights.query(xid,FLIGHTS,reservation.getResvKey());
                        flight.setNumAvail(flight.getNumAvail() + 1);
                        rmFlights.update(xid,FLIGHTS,flight.getFlightNum(),flight);
                        rmCustomers.delete(xid,RESERVATIONS,key);
                    } else if (reservation.getResvType() == Reservation.RESERVATION_TYPE_CAR) {
                        Car car = (Car) rmCars.query(xid,CARS,reservation.getResvKey());
                        car.setNumAvail(car.getNumAvail() + 1);
                        rmCars.update(xid,CARS,car.getLocation(),car);
                        rmCustomers.delete(xid,RESERVATIONS,key);
                    } else if (reservation.getResvType() == Reservation.RESERVATION_TYPE_HOTEL) {
                        Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,reservation.getResvKey());
                        hotel.setNumAvail(hotel.getNumAvail() + 1);
                        rmRooms.update(xid,HOTELS,hotel.getLocation(),hotel);
                        rmCustomers.delete(xid,RESERVATIONS,key);
                    } else {
                        System.err.println("wrong reservation type");
                    }
                }
            }
            return rmCustomers.delete(xid,CUSTOMERS,custName);
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        } catch (InvalidIndexException e) {
            throw new InvalidTransactionException(xid,e.getMessage());
        }
    }

    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (flightNum == null || flightNum.isEmpty()) {
            return -1;
        }
        try {

            Flight flight = (Flight) rmFlights.query(xid,FLIGHTS,flightNum);
            if (flight == null) {
                return -1;
            }
            return flight.getNumAvail();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (flightNum == null || flightNum.isEmpty()) {
            return -1;
        }
        try {
            Flight flight = (Flight) rmFlights.query(xid,FLIGHTS,flightNum);
            if (flight == null) {
                return -1;
            }
            return flight.getNumAvail();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryRooms(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (location == null || location.isEmpty()) {
            return -1;
        }
        try {
            Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,location);
            if (hotel == null) {
                return -1;
            }
            return hotel.getNumAvail();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (location == null || location.isEmpty()) {
            return -1;
        }
        try {
            Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,location);
            if (hotel == null) {
                return -1;
            }
            return hotel.getPrice();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryCars(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (location == null || location.isEmpty()) {
            return -1;
        }
        try {
            Car car = (Car) rmCars.query(xid,CARS,location);
            if (car == null) {
                return -1;
            }
            return car.getNumAvail();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        if (location == null || location.isEmpty()) {
            return -1;
        }
        try {
            Car car = (Car) rmCars.query(xid,CARS,location);
            if (car == null) {
                return -1;
            }

            return car.getPrice();
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return -1;
        }
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        // get all reservations of this customer
        if (custName == null) {
            return -1;
        }
        int bill = 0;
        try {
            Customer customer = (Customer) rmCustomers.query(xid,CUSTOMERS,custName);
            if (customer == null) {
                return -1;
            }
            Collection reservationList = rmCustomers.query(xid,RESERVATIONS,"custName",custName);
            if (reservationList == null) {
                return 0;
            }
            for (Object i : reservationList) {
                Reservation reservation = (Reservation) i;
                if (reservation.getResvType() == Reservation.RESERVATION_TYPE_FLIGHT) {
                    Flight flight = (Flight) rmFlights.query(xid,FLIGHTS,reservation.getResvKey());
                    bill += flight.getPrice();
                } else if (reservation.getResvType() == Reservation.RESERVATION_TYPE_CAR) {
                    Car car = (Car) rmCars.query(xid,CARS,reservation.getResvKey());

                    bill += car.getPrice();
                } else if (reservation.getResvType() == Reservation.RESERVATION_TYPE_HOTEL) {
                    Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,reservation.getResvKey());
                    bill += hotel.getPrice();
                }
            }
        }  catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            throw new RuntimeException(e);
        } catch (InvalidIndexException e) {
            throw new InvalidTransactionException(xid,e.getMessage());
        }
        return bill;
    }

    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Flight flight = (Flight) rmFlights.query(xid,FLIGHTS,flightNum);
            if (flight == null) return false;
            flight.setNumAvail(flight.getNumAvail() - 1);
            if (!flight.isVaild()) return false;
            rmFlights.update(xid,FLIGHTS,flight.getFlightNum(),flight);
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT,flightNum);
            rmCustomers.insert(xid,RESERVATIONS,reservation);
            return true;
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Car car = (Car) rmCars.query(xid,CARS,location);
            if (car == null) return false;
            car.setNumAvail(car.getNumAvail() - 1);
            if (!car.isVaild()) return false;
            rmCars.update(xid,CARS,car.getLocation(),car);
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_CAR,location);
            rmCustomers.insert(xid,RESERVATIONS,reservation);

            return true;
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid, e);
            return false;
        }
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException,
            TransactionAbortedException,
            InvalidTransactionException {
        try {
            Hotel hotel = (Hotel) rmRooms.query(xid,HOTELS,location);
            if (hotel == null) return false;
            hotel.setNumAvail(hotel.getNumAvail() - 1);
            if (!hotel.isVaild()) return false;
            rmRooms.update(xid,HOTELS,hotel.getLocation(),hotel);
            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_HOTEL,location);
            rmCustomers.insert(xid,RESERVATIONS,reservation);

            return true;
        } catch (DeadlockException e) {
            errorHandleDeadlock(xid,e);
            return false;
        }


    }

    private String getLookupStr (Properties prop, String rmiName) {
        String rmiPort = prop.getProperty("rm." + rmiName + ".port");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }
        return rmiPort + rmiName;
    }

    private String getLookupStrTm (Properties prop) {
        String rmiPort = prop.getProperty("tm.port");
        if (rmiPort == null) {
            rmiPort = "";
        } else if (!rmiPort.equals("")) {
            rmiPort = "//:" + rmiPort + "/";
        }
        return rmiPort + TransactionManager.RMIName;
    }

    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect()
            throws RemoteException {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/ddb.conf"));
        } catch (Exception e1) {
            e1.printStackTrace();
            return false;
        }

        try {
            String q;
            q = this.getLookupStr(prop, ResourceManager.RMINameFlights);
            rmFlights = (ResourceManager) Naming.lookup(q);
            System.out.println("WC bound to RMFlights");

            q = this.getLookupStr(prop, ResourceManager.RMINameRooms);
            rmRooms = (ResourceManager) Naming.lookup(q);
            System.out.println("WC bound to RMRooms");

            q = this.getLookupStr(prop, ResourceManager.RMINameCars);
            rmCars = (ResourceManager) Naming.lookup(q);
            System.out.println("WC bound to RMCars");

            q = this.getLookupStr(prop, ResourceManager.RMINameCustomers);
            rmCustomers = (ResourceManager) Naming.lookup(q);
            System.out.println("WC bound to RMCustomers");

            q = this.getLookupStrTm(prop);
            tm = (TransactionManager) Naming.lookup(q);
            System.out.println("WC bound to TM");
        } catch (Exception e) {
            System.err.println("WC cannot bind to some component:" + e);
            return false;
        }

        try {
            if (rmFlights.reconnect() && rmRooms.reconnect() &&
                    rmCars.reconnect() && rmCustomers.reconnect()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Some RM cannot reconnect:" + e);
            return false;
        }
        System.out.println("should not go here");
        return false;
    }

    public boolean dieNow(String who)
            throws RemoteException {
        if (who.equals(TransactionManager.RMIName) ||
                who.equals("ALL")) {
            try {
                tm.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameFlights) ||
                who.equals("ALL")) {
            try {
                rmFlights.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameRooms) ||
                who.equals("ALL")) {
            try {
                rmRooms.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCars) ||
                who.equals("ALL")) {
            try {
                rmCars.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(ResourceManager.RMINameCustomers) ||
                who.equals("ALL")) {
            try {
                rmCustomers.dieNow();
            } catch (RemoteException e) {
            }
        }
        if (who.equals(WorkflowController.RMIName) ||
                who.equals("ALL")) {
            System.exit(1);
        }
        return true;
    }

    public boolean dieRMAfterEnlist(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforePrepare(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMAfterPrepare(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieTMBeforeCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieTMAfterCommit()
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforeCommit(String who)
            throws RemoteException {
        return true;
    }

    public boolean dieRMBeforeAbort(String who)
            throws RemoteException {
        return true;
    }

    private void errorHandleDeadlock(int xid, DeadlockException e)
            throws TransactionAbortedException, InvalidTransactionException, RemoteException
    {
        abort(xid);
        throw new TransactionAbortedException(xid,e.getMessage());
    }
}
