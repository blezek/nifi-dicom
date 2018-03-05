/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.SetOfDicomFiles;

/**
 * <p>This interface abstracts the communication between a retrieve (C-MOVE) server
 * that implements the network service, and the source of information with which
 * to respond to the retrieve request.</p>
 *
 * <p>Typically, the server which has been supplied a factor to generate instances
 * of a class that implements this interface, calls the methods with the following pattern:</p>
<pre>
	... receive C-MOVE request from the network into AttributeList identifier ...
	RetrieveResponseGenerator retrieveResponseGenerator = retrieveResponseGeneratorFactory.newInstance();
	retrieveResponseGenerator.performRetrieve(affectedSOPClassUID,identifier,false);
	SetOfDicomFiles dicomFiles = retrieveResponseGenerator.getDicomFiles();
	int status = retrieveResponseGenerator.getStatus();
	AttributeTagAttribute badElement = retrieveResponseGenerator.getOffendingElement();
	retrieveResponseGenerator.close();
	... send C-MOVE response ...
</pre>
 *
 * @see com.pixelmed.query.RetrieveResponseGeneratorFactory
 *
 * @author	dclunie
 */
public interface RetrieveResponseGenerator {

	/**
	 * <p>Perform the retrieve whose results will be returned by the getDicomFiles() method.</p>
	 *
	 * @param	retrieveSOPClassUID	defines the information model to use (e.g., study root, patient root)
	 * @param	requestIdentifier	the request itself, as received in the C-MOVE dataset
	 * @param	relational			whether or not to perform relational queries (as determined by extended negotiation)
	 */
	public void performRetrieve(String retrieveSOPClassUID,AttributeList requestIdentifier,boolean relational);
	
	/**
	 * <p>Get the list of files that satisfy the entire retrieval request.</p>
	 *
	 * @return	the list of files to be retrieved
	 */
	public SetOfDicomFiles getDicomFiles();
	
	/**
	 * <p>Get a status suitable for the C-MOVE response.</p>
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
	 * <p>Clean up after the retrieval has completed.</p>
	 *
	 * <p>Typically, will discarded cached information, close any database connections, etc.</p>
	 */
	public void close();
}
