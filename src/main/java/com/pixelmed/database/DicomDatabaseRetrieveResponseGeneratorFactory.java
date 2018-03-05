/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.query.RetrieveResponseGenerator;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

class DicomDatabaseRetrieveResponseGeneratorFactory implements RetrieveResponseGeneratorFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDatabaseRetrieveResponseGeneratorFactory.java,v 1.5 2017/01/24 10:50:35 dclunie Exp $";

	/***/
	private DatabaseInformationModel databaseInformationModel;

	DicomDatabaseRetrieveResponseGeneratorFactory(DatabaseInformationModel databaseInformationModel) {
//System.err.println("DicomDatabaseRetrieveResponseGeneratorFactory():");
		this.databaseInformationModel=databaseInformationModel;
	}
	
	public RetrieveResponseGenerator newInstance() {
		return new DicomDatabaseRetrieveResponseGenerator(databaseInformationModel);
	}

}

