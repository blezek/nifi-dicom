/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

/**
 * @author	dclunie
 */
public class ResponseStatus {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/ResponseStatus.java,v 1.4 2017/01/24 10:50:46 dclunie Exp $";

	static public int Success = 0x0000;

	// Failure
	
	static public int RefusedOutOfResourcesUnableToCalculateNumberOfMatches = 0xA701;		// (0000,0902)
	static public int RefusedOutOfResourcesUnableToPerformSubOperations = 0xA702;			// (0000,1020),(0000,1021),(0000,1022),(0000,1023)
	static public int RefusedMoveDestinationUnknown = 0xA801;								// (0000,0902)
	static public int IdentifierDoesNotMatchSOPClass = 0xA900;								// (0000,0901), (0000,0902)
	static public int UnableToProcess = 0xC000;												// (0000,0901), (0000,0902)
	
	// Cancel
	
	static public int SubOperationsTerminatedDueToCancelIndication = 0xFE00;				// (0000,1020),(0000,1021),(0000,1022),(0000,1023)
	static public int MatchingTerminatedDueToCancelIndication = 0xFE00;
	
	// Warning
	
	static public int SubOperationsCompleteOneOrMoreFailures = 0xB000;						// (0000,1020),(0000,1022),(0000,1023)
	static public int SubOperationsCompleteNoFailures = 0x0000;								// (0000,1020),(0000,1021),(0000,1022),(0000,1023)

	// Pending
	
	static public int SubOperationsAreContinuing = 0xFF00;
	static public int MatchesAreContinuingOptionalKeysSupported = 0xFF00;
	static public int MatchesAreContinuingOptionalKeysNotSupported = 0xFF01;

}
