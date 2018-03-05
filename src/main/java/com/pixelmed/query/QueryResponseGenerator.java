/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTagAttribute;

/**
 * <p>This interface abstracts the communication between a query (C-FIND) server
 * that implements the network service, and the source of information with which
 * to respond to the query.</p>
 *
 * <p>Typically, the server, which has been supplied a factory to generate instances
 * of a class that implements this interface, calls the methods with the following pattern:</p>
<pre>
	... receive C-FIND request from the network into AttributeList identifier ...
	QueryResponseGenerator queryResponseGenerator = queryResponseGeneratorFactory.newInstance();
	queryResponseGenerator.performQuery(affectedSOPClassUID,identifier,false);
	AttributeList responseIdentifierList;
	while ((responseIdentifierList = queryResponseGenerator.next()) != null) {
		... send response in responseIdentifierList over the network ...
	}
	queryResponseGenerator.close();
</pre>
 *
 * @see com.pixelmed.query.QueryResponseGeneratorFactory
 *
 * @author	dclunie
 */
public interface QueryResponseGenerator {
	
	/**
	 * <p>Perform the query whose results will be returned by the next() method.</p>
	 *
	 * <p>Whether or not the query is actually performed during this call, or the parameters cached and
	 * perform on the first next() call, or indeed next() represents a dynamic query, is unspecified
	 * and up to the implementor of the interface.</p>
	 *
	 * @param	querySOPClassUID	defines the information model to use (e.g., study root, patient root)
	 * @param	queryIdentifier		the query itself, as received in the C-FIND dataset
	 * @param	relational			whether or not to perform relational queries (as determined by extended negotiation)
	 */
	public void performQuery(String querySOPClassUID,AttributeList queryIdentifier,boolean relational);
	
	/**
	 * <p>Get the next query response.</p>
	 *
	 * @return	the next query response as a list of DICOM attributes suitable for a C-FIND response dataset
	 */
	public AttributeList next();
	
	/**
	 * <p>Get a status suitable for the C-FIND response.</p>
	 *
	 * @return	the retrieve status
	 */
	public int getStatus();

	/**
	 * <p>Get the offending element in the case of an error status.</p>
	 *
	 * @return	the offending element
	 */
	public AttributeTagAttribute getOffendingElement();

	/**
	 * <p>Get any error comment in the case of an error status.</p>
	 *
	 * @return	the error comment, or null if none
	 */
	public String getErrorComment();
	
	/**
	 * <p>Clean up after the query has completed.</p>
	 *
	 * <p>Typically, will discarded cached information, close any database connections, etc.</p>
	 */
	public void close();
	
	/**
	 * <p>Determine information needed to qualify pending response statuses.</p>
	 *
	 * @return	false if any of the keys in the queryIdentifier were not supported as matching or return keys by the model
	 */
	public boolean allOptionalKeysSuppliedWereSupported();
}
