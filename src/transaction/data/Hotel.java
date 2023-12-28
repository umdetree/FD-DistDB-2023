
package transaction.data;

import transaction.InvalidIndexException;
import transaction.ResourceItem;

import java.io.Serializable;

public class Hotel implements ResourceItem, Serializable {
    public static final String INDEX_Hol = "location";
    protected String location;
    protected int price;
    protected int numRooms;
    protected int numAvail;
    protected boolean isdeleted = false;

    public Hotel(String location, int price, int numRooms, int numAvail) {
        this.location = location;
        this.price = price;
        this.numRooms = numRooms;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"location", "price", "numRooms", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{this.location, "" + this.price, "" + this.numRooms, "" + this.numAvail};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_Hol)) {
            return this.location;
        } else {
            throw new InvalidIndexException(indexName);
        }
    }

    public Object getKey() {
        return this.location;
    }

    public boolean isDeleted() {
        return this.isdeleted;
    }

    public void delete() {
        this.isdeleted = true;
    }

    public Object clone() {
        Hotel o = new Hotel(this.location, this.price, this.numRooms, this.numAvail);
        o.isdeleted = this.isdeleted;
        return o;
    }

    public String getLocation() {
        return this.location;
    }

    public int getNumAvail() {
        return this.numAvail;
    }

    public void setNumAvail(int numAvail) {
        this.numAvail = numAvail;
    }

    public int getNumRooms() {
        return this.numRooms;
    }

    public void setNumRooms(int numRooms) {
        this.numRooms = numRooms;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getPrice() {
        return this.price;
    }


    public boolean isVaild() {
    	return this.numRooms >= 0 && this.price >= 0 && location!= null && !location.isEmpty();
    }
}