
package transaction.data;

import transaction.InvalidIndexException;
import transaction.ResourceItem;

import java.io.Serializable;

public class Car implements ResourceItem, Serializable {
    public static final String INDEX_Car = "location";
    protected String location;
    protected int price;
    protected int numCars;
    protected int numAvail;
    protected boolean isdeleted = false;

    public Car(String location, int price, int numCars, int numAvail) {
        this.location = location;
        this.price = price;
        this.numCars = numCars;
        this.numAvail = numAvail;
    }

    public String[] getColumnNames() {
        return new String[]{"location", "price", "numCars", "numAvail"};
    }

    public String[] getColumnValues() {
        return new String[]{this.location, "" + this.price, "" + this.numCars, "" + this.numAvail};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_Car)) {
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
        Car o = new Car(this.location, this.price, this.numCars, this.numAvail);
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

    public int getNumCars() {
        return numCars;
    }

    public void setNumCars(int numCars) {
        this.numCars = numCars;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }

    public boolean isVaild() {
        return numCars >= 0 && price >= 0 && location!= null && !location.isEmpty();
    }
}