/*
 * Created on 2005-5-17
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package transaction;

/**
 * @author RAdmin
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window -
 *         Preferences - Java - Code Style - Code Templates
 */
public class InvalidIndexException extends Exception {
    public InvalidIndexException(String indexName) {
        super("invalid index: " + indexName);
    }
}
