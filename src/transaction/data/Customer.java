
package transaction.data;

import transaction.InvalidIndexException;
import transaction.ResourceItem;

import java.io.Serializable;

public class Customer implements ResourceItem, Serializable {
    public static final String INDEX_Cus = "custName";
    protected String custName;
    protected boolean isdeleted = false;

    public Customer(String custName) {
        this.custName = custName;
    }

    public String[] getColumnNames() {
        return new String[]{"custName"};
    }

    public String[] getColumnValues() {
        return new String[]{this.custName};
    }

    public Object getIndex(String indexName) throws InvalidIndexException {
        if (indexName.equals(INDEX_Cus)) {
            return this.custName;
        } else {
            throw new InvalidIndexException(indexName);
        }
    }

    public Object getKey() {
        return this.custName;
    }

    public boolean isDeleted() {
        return this.isdeleted;
    }

    public void delete() {
        this.isdeleted = true;
    }

    public Object clone() {
        Customer o = new Customer(this.custName);
        o.isdeleted = this.isdeleted;
        return o;
    }

    public String getCustName() {
        return this.custName;
    }

}