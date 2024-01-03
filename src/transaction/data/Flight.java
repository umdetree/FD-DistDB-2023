
package transaction.data;

import java.io.Serializable;
import transaction.InvalidIndexException;
import transaction.ResourceItem;

public class Flight implements ResourceItem, Serializable {
    public static final String INDEX_Fli = "flightNum";
    protected String flightNum;
    protected int price;
    protected int numSeats;
    protected int numAvail;
    protected boolean isdeleted = false;

    public Flight(String flightNum, int price, int numSeats, int numAvail) {
        this.flightNum = flightNum;
        this.price = price;
        this.numSeats = numSeats;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"flightNum", "price", "numSeats", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{this.flightNum, "" + this.price, "" + this.numSeats, "" + this.numAvail};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_Fli)) {
            return this.flightNum;
        } else {
            throw new InvalidIndexException(indexName);
        }
    }

    public Object getKey() {
        return this.flightNum;
    }

    public boolean isDeleted() {
        return this.isdeleted;
    }

    public void delete() {
        this.isdeleted = true;
    }

    public Object clone() {
        Flight o = new Flight(this.flightNum, this.price, this.numSeats, this.numAvail);
        o.isdeleted = this.isdeleted;
        return o;
    }

    public String getFlightNum() {
        return this.flightNum;
    }

    public int getNumAvail() {
        return this.numAvail;
    }

    public void setNumAvail(int numAvail) {
        this.numAvail = numAvail;
    }

    public int getNumSeats() {
        return this.numSeats;
    }

    public void setNumSeats(int numSeats) {
        this.numSeats = numSeats;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getPrice() {
        return this.price;
    }
    public boolean isVaild() {
        return numSeats>= 0 && price >= 0 && flightNum!= null && !flightNum.isEmpty();
    }
}