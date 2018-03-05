/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.*;
import com.pixelmed.query.QueryResponseGeneratorFactory;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

import java.sql.*;
import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link DatabaseInformationModel DatabaseInformationModel} class is an abstract class that contains the core
 * functionality for storing, accessing and maintaining a persistent representation of selected attributes of composite
 * objects.</p>
 *
 * <p>It hides an underlying SQL database implementation that stores the attributes of each
 * entity in the information model in tables.</p>
 *
 * <p>Abstract sub-classes, such as {@link com.pixelmed.database.DicomDatabaseInformationModel DicomDatabaseInformationModel},
 * may refine the type of information model supported, since this base class is fairly generic and not dependent on any
 * particular DICOM information model. Concrete sub-classes define particular models by overriding the methods that
 * define the root entities and the relationship between entities, as well as the unique keys for each particular
 * entity.</p>
 *
 * <p>In addition, the concrete sub-classes define which attributes of each entity will be included in the persistent
 * representation (the database tables), usually by means of a specialized sub-class of {@link com.pixelmed.dicom.DicomDictionary DicomDictionary}
 * which contains the sub-set of relevant attributes and may change their mapping to information entities, such as
 * {@link com.pixelmed.database.DicomDictionaryForPatientStudySeriesConcatenationInstanceModel DicomDictionaryForPatientStudySeriesConcatenationInstanceModel}.</p>
 *
 * <p>For example, an application might instantiate a {@link com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel PatientStudySeriesConcatenationInstanceModel}
 * or a {@link com.pixelmed.database.StudySeriesInstanceModel StudySeriesInstanceModel}, as follows:</p>
 *
 * <pre>
 *	final DatabaseInformationModel d = new PatientStudySeriesConcatenationInstanceModel("test");
 * </pre>
 *
 * <p>Composite objects previously read into a {@link com.pixelmed.dicom.AttributeList AttributeList} from a
 * DICOM file in a persistent location could be inserted into the persistent representation (the database)
 * as follows, indicating in this case that the file was copied (and may later be deleted) rather than referenced:</p>
 *
 * <pre>
 *	d.insertObject(list,fileName,DatabaseInformationModel.FILE_COPIED);
 * </pre>
 *
 * <p>External (unsecure) SQL access to the database is possible if a databaseServerName argument is supplied in the constructor, in which
 * case a tool like {@link org.hsqldb.util.DatabaseManagerSwing org.hsqldb.util.DatabaseManagerSwing} (described in detail at <a href="http://www.hsqldb.org/doc/guide/apf.html">Hsqldb User Guide: Appendix F. Database Manager</a>) can be used to query or manage the database. For example:</p>
 * <pre>
% java -cp lib/additional/hsqldb.jar org.hsqldb.util.DatabaseManagerSwing --url "jdbc:hsqldb:hsql://hostname/databaseServerName"
 * </pre>
 *
 * <p>where "hostname" is the name of the host (perhaps "localhost"), and "databaseServerName" is the name supplied in the constructor.</p>
 *
 * <p>Note that the default username ("sa") and password (empty) are not secure.</p>
 *
 * @see com.pixelmed.database
 *
 * @author	dclunie
 */
public abstract class DatabaseInformationModel {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DatabaseInformationModel.java,v 1.77 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(DatabaseInformationModel.class);

	public static final String FILE_COPIED = "C";
	public static final String FILE_REFERENCED = "R";
	
	/***/
	protected static final String defaultDatabaseRootName = "Local database";		// e.g., as used in root node of TreeModel
	/***/
	protected static final String localPrimaryKeyColumnName = "LOCALPRIMARYKEY";	// needs to be upper case
	/***/
	protected static final String localParentReferenceColumnName = "LOCALPARENTREFERENCE";	// needs to be upper case
	/***/
	protected static final String localRecordInsertionTimeColumnName = "RECORDINSERTIONTIME";	// needs to be upper case
	/***/
	protected static final String localFileName = "LOCALFILENAME";	// needs to be upper case
	/***/
	protected static final String localFileReferenceTypeColumnName = "LOCALFILEREFERENCETYPE";	// needs to be upper case
	/***/
	protected static final String personNameCanonicalColumnNamePrefix = "PM_";	// needs to be upper case
	/***/
	protected static final String personNameCanonicalColumnNameSuffix = "_CANONICAL";	// needs to be upper case
	/***/
	protected static final String personNamePhoneticCanonicalColumnNamePrefix = "PM_";	// needs to be upper case
	/***/
	protected static final String personNamePhoneticCanonicalColumnNameSuffix = "_PHONETICCANONICAL";	// needs to be upper case
	/***/
	protected static final String userColumnName1 = "PM_USER1";	// needs to be upper case
	/***/
	protected static final String userColumnName2 = "PM_USER2";	// needs to be upper case
	/***/
	protected static final String userColumnName3 = "PM_USER3";	// needs to be upper case
	/***/
	protected static final String userColumnName4 = "PM_USER4";	// needs to be upper case

	/***/
	protected InformationEntity rootInformationEntity;
	/***/
	protected HashMap listsOfAttributesByInformationEntity;
	/***/
	protected HashMap additionalIndexMapOfColumnsToTables;
	/***/
	protected Connection databaseConnection;
	/***/
	protected org.hsqldb.Server externalServerInstance;
	
	/***/
	protected DicomDictionary dictionary;

	/***/
	DicomDictionary getDicomDictionary() { return dictionary; }

	/***/
	protected String databaseRootName;

	/***/
	String getDatabaseRootName() { return databaseRootName; }

	/***/
	private Map descriptiveNameMap;

	/***/
	private HashSet localColumnExcludeList;

	/***/
	HashSet getLocalColumnExcludeList() {			// package scope ... used by DatabaseTreeBrowser
		return localColumnExcludeList;
	}

	/**
	 * <p>Return a list of (upper case) column names not to include in human-readable renderings like browsers.</p>
	 * 
	 * <p>Includes such things as the internal primary and parent keys, record insertion time, local file names.</p>
	 */
	/*protected*/ void makeLocalColumnExcludeList() {		// protected scope ... may be (but isn't currently) overridden by specialized classes
		localColumnExcludeList = new HashSet();
		localColumnExcludeList.add(localPrimaryKeyColumnName);
		localColumnExcludeList.add(localParentReferenceColumnName);
		localColumnExcludeList.add(localRecordInsertionTimeColumnName);
		localColumnExcludeList.add(localFileName);
		localColumnExcludeList.add(localFileReferenceTypeColumnName);
	}
	
	/**
	 * <p>Instantiate a persistent information model using the named database.</p>
	 *
	 * <p>Will open a connection to the database, and create any tables if this is the first time,
	 * or re-use any persistent information if not.</p>
	 *
	 * @param	databaseFileName		the file name of the underlying SQL database instance to be used
	 * @param	rootInformationEntity	the top entity of the information model; specific to a particular model's constructor
	 * @param	dictionary				used to decide which attributes to include for each entity when creating the tables
	 * @throws	DicomException		thrown if a connection to the database cannot be established
	 */
	public DatabaseInformationModel(String databaseFileName,InformationEntity rootInformationEntity,DicomDictionary dictionary) throws DicomException {
		doCommonConstructorStuff(databaseFileName,null,rootInformationEntity,dictionary,defaultDatabaseRootName);
	}

	/**
	 * <p>Instantiate a persistent information model using the named database.</p>
	 *
	 * <p>Will open a connection to the database, and create any tables if this is the first time,
	 * or re-use any persistent information if not.</p>
	 *
	 * @param	databaseFileName		the file name of the underlying SQL database instance to be used
	 * @param	rootInformationEntity	the top entity of the information model; specific to a particular model's constructor
	 * @param	dictionary				used to decide which attributes to include for each entity when creating the tables
	 * @param	databaseRootName		the name used for the root node of the database in TreeModel
	 * @throws	DicomException		thrown if a connection to the database cannot be established
	 */
	public DatabaseInformationModel(String databaseFileName,InformationEntity rootInformationEntity,DicomDictionary dictionary,String databaseRootName) throws DicomException {
		doCommonConstructorStuff(databaseFileName,null,rootInformationEntity,dictionary,databaseRootName);
	}

	/**
	 * <p>Instantiate a persistent information model using the named database allowing external SQL access.</p>
	 *
	 * <p>Will open a connection to the database, and create any tables if this is the first time,
	 * or re-use any persistent information if not.</p>
	 *
	 * @param	databaseFileName		the file name of the underlying SQL database instance to be used
	 * @param	databaseServerName		the name to use for external TCP access to database (such a server will not be started if this value is null or zero length)
	 * @param	rootInformationEntity	the top entity of the information model; specific to a particular model's constructor
	 * @param	dictionary				used to decide which attributes to include for each entity when creating the tables
	 * @throws	DicomException		thrown if a connection to the database cannot be established
	 */
	public DatabaseInformationModel(String databaseFileName,String databaseServerName,InformationEntity rootInformationEntity,DicomDictionary dictionary) throws DicomException {
		doCommonConstructorStuff(databaseFileName,databaseServerName,rootInformationEntity,dictionary,defaultDatabaseRootName);
	}

	/**
	 * <p>Instantiate a persistent information model using the named database allowing external SQL access.</p>
	 *
	 * <p>Will open a connection to the database, and create any tables if this is the first time,
	 * or re-use any persistent information if not.</p>
	 *
	 * @param	databaseFileName		the file name of the underlying SQL database instance to be used
	 * @param	databaseServerName		the name to use for external TCP access to database (such a server will not be started if this value is null or zero length)
	 * @param	rootInformationEntity	the top entity of the information model; specific to a particular model's constructor
	 * @param	dictionary				used to decide which attributes to include for each entity when creating the tables
	 * @param	databaseRootName		the name used for the root node of the database in TreeModel
	 * @throws	DicomException		thrown if a connection to the database cannot be established
	 */
	public DatabaseInformationModel(String databaseFileName,String databaseServerName,InformationEntity rootInformationEntity,DicomDictionary dictionary,String databaseRootName) throws DicomException {
		doCommonConstructorStuff(databaseFileName,databaseServerName,rootInformationEntity,dictionary,databaseRootName);
	}

	/**
	 * <p>Instantiate a persistent information model using the named database.</p>
	 *
	 * <p>Will open a connection to the database, and create any tables if this is the first time,
	 * or re-use any persistent information if not.</p>
	 *
	 * @param	databaseFileName		the file name of the underlying SQL database instance to be used (or "mem:name" if the database is to be in-memory and not persisted)
	 * @param	databaseServerName		the name to use for external TCP access to database (such a server will not be started if this value is null or zero length)
	 * @param	rootInformationEntity	the top entity of the information model; specific to a particular model's constructor
	 * @param	dictionary				used to decide which attributes to include for each entity when creating the tables
	 * @param	databaseRootName		the name used for the root node of the database in TreeModel
	 * @throws	DicomException		thrown if a connection to the database cannot be established
	 */
	protected void doCommonConstructorStuff(String databaseFileName,String databaseServerName,InformationEntity rootInformationEntity,DicomDictionary dictionary,String databaseRootName) throws DicomException {
//System.err.println("DatabaseInformationModel.doCommonConstructorStuff(): databaseFileName = "+databaseFileName);
		this.databaseRootName=databaseRootName;
		this.rootInformationEntity=rootInformationEntity;
		this.dictionary=dictionary;
		if (this.dictionary == null) {
//System.err.println("DatabaseInformationModel.doCommonConstructorStuff(): no dictionary supplied so using default");
			this.dictionary = new DicomDictionary();
		}
//		else {
//System.err.println("DatabaseInformationModel.doCommonConstructorStuff(): dictionary class = "+dictionary.getClass());
//		}
		listsOfAttributesByInformationEntity = new HashMap(6);
		additionalIndexMapOfColumnsToTables = new HashMap();
		makeLocalColumnExcludeList();
		try {
			Class.forName("org.hsqldb.jdbcDriver").newInstance();
			java.util.Properties properties = new java.util.Properties();
			properties.put("user","sa");
			properties.put("password","");
			properties.put("hsqldb.cache_file_scale","8");			// default is only 2GB; must be set BEFORE any cached tables are created
			properties.put("sql.enforce_size","FALSE");				// default is true with 2.x
			properties.put("sql.enforce_strict_size","FALSE");		// default is true with 2.x
			databaseConnection=DriverManager.getConnection("jdbc:hsqldb:"+databaseFileName,properties);
//System.err.println("DatabaseInformationModel(): first call to primeListsOfAttributesByInformationEntityFromExistingMetaData() to see if tables exist");
			primeListsOfAttributesByInformationEntityFromExistingMetaData();
			if (listsOfAttributesByInformationEntity.size() == 0) {
//System.err.println("DatabaseInformationModel(): our tables do not exist, create them");
				createTables();
//System.err.println("DatabaseInformationModel(): second call to primeListsOfAttributesByInformationEntityFromExistingMetaData() now that we have added our tables");
				primeListsOfAttributesByInformationEntityFromExistingMetaData();
			}
			createDescriptiveNameMap();
		}
		catch (Exception e) {
			throw new DicomException("Cannot connect to database: "+e);
		}
		if (databaseServerName != null && databaseServerName.trim().length() > 0) {
//System.err.println("DatabaseInformationModel.doCommonConstructorStuff(): attempting to start external database server named = "+databaseServerName);
			try {
				String serverProperties="database.0="+databaseFileName+";dbname.0="+databaseServerName;
				externalServerInstance = new org.hsqldb.Server();
				externalServerInstance.setLogWriter(null);
				externalServerInstance.setErrWriter(null);
				externalServerInstance.putPropertiesFromString(serverProperties);
				externalServerInstance.start();
			}
			catch (Exception e) {
				throw new DicomException("Cannot create additional server instance of database for external access: "+e);
			}
		}
		else {
			externalServerInstance=null;
		}
	}

	/**
	 * <p>Close the underlying connection to the database and shutdown any external SQL server.</p>
	 *
	 * <p>Prior to actually closing will also try to formally shutdown and compact the database.</p>
	 */
	public void close() {
		if (externalServerInstance != null) {
//System.err.println("DatabaseInformationModel.close(): externalServerInstance shutdown start");
			externalServerInstance.shutdown();
//System.err.println("DatabaseInformationModel.close(): externalServerInstance shutdown finished");
			externalServerInstance = null;
		}
		if (databaseConnection != null) {
//System.err.println("DatabaseInformationModel.close(): shutdown compact start");
			try {
				Statement s = databaseConnection.createStatement();
				s.execute("SHUTDOWN COMPACT;");	// no ResultSet expected
				s.close();
			} catch (SQLException e) {
				slf4jlogger.error("Ignoring exception during database shutdown compact",e);
			}
//System.err.println("DatabaseInformationModel.close(): shutdown compact finished");
			try {
				databaseConnection.close();
			}
			catch (SQLException e) {
				slf4jlogger.error("Ignoring exception during database close",e);
			}
			databaseConnection=null;
		}
	}

	/***/
	protected void finalize() {
		close();
	}
	
	/***/
	Statement createStatement() throws java.sql.SQLException {
		return databaseConnection.createStatement();
	}
	
	/**
	 * <p>Get the entity that is at the top of the information model.</p>
	 *
	 * <p>May be used, for example, to begin top-down traversal of a composite object, or the entire database.</p>
	 *
	 * <p>Will be specific to a particular concrete information model.</p>
	 *
	 * @return	the {@link com.pixelmed.dicom.InformationEntity InformationEntity} at the top of the information model
	 */
	InformationEntity getRootInformationEntity() { return rootInformationEntity; }

	/**
	 * <p>Is the entity in the information model ? </p>
	 *
	 * <p>Will be specific to a particular concrete information model.</p>
	 *
	 * @return	boolean		the information entity is in the information model
	 */
	protected abstract boolean isInformationEntityInModel(InformationEntity ie);

	/**
	 * <p>Is the named entity in the information model ? </p>
	 *
	 * <p>Will be specific to a particular concrete information model.</p>
	 *
	 * @param	ieName		a String name, whose case is ignored (e.g., a database table named for the corresponding information entity)
	 * @return			the name corresponds to an information entity that is in the information model
	 */
	protected boolean isInformationEntityInModel(String ieName) {
		InformationEntity ie = InformationEntity.fromString(ieName);
		return ie == null ? false : isInformationEntityInModel(ie);
	}

	/**
	 * <p>Get the map of attribute (table column) names to descriptive names.</p>
	 *
	 * <p>May be used, for example, for column header labels in table browsers.</p>
	 *
	 * @return	a {@link java.util.HashMap HashMap} indexed by {@link java.lang.String String} upper case column names and containing {@link java.lang.String String} descriptive names
	 */
	Map getDescriptiveNameMap() { return descriptiveNameMap; }

	/**
	 * @throws	DicomException
	 */
	private void createDescriptiveNameMap() throws DicomException {
		descriptiveNameMap = new HashMap();
		Iterator i = dictionary.getTagIterator();
		while (i.hasNext()) {
			AttributeTag tag = (AttributeTag)i.next();
			// assert dictionary != null;
			String descriptiveName = dictionary.getNameFromTag(tag);
			String columnName = getDatabaseColumnNameFromDicomName(descriptiveName);
			if (descriptiveName != null && columnName != null) {
				descriptiveNameMap.put(columnName,descriptiveName);
			}
		}
	}
	
	/**
	 * <p>Get the name of the table column corresponding to the DICOM tag.</p>
	 *
	 * @param	tag		the tag of the DICOM element
	 * @return			a {@link java.lang.String String} column name, or null if not known
	 */
	public String getDatabaseColumnNameFromDicomTag(AttributeTag tag) {
		String columnName = null;
		if (dictionary != null) {
			columnName = getDatabaseColumnNameFromDicomName(dictionary.getNameFromTag(tag));
		}
		return columnName;
	}
	
	/**
	 * <p>Get the name of the table column corresponding to the DICOM element name.</p>
	 *
	 * @param	descriptiveName		the name of the DICOM element
	 * @return				the upper case {@link java.lang.String String} column name, or null if not known
	 */
	public static String getDatabaseColumnNameFromDicomName(String descriptiveName) {
		String columnName = null;
		if (descriptiveName != null) {
			columnName = descriptiveName.toUpperCase(java.util.Locale.US);
		}
		return columnName;
	}

	/**
	 * <p>Get the name of the DICOM element corresponding to the database table column.</p>
	 *
	 * @param	columnName		the name of the database column
	 * @return				a {@link java.lang.String String} descriptive name of the DICOM element, or null if not known
	 */
	public String getDicomNameFromDatabaseColumnName(String columnName) {
		String descriptiveName = null;
		if (columnName != null) {
			descriptiveName = (String)(descriptiveNameMap.get(columnName));
		}
		return descriptiveName;
	}

	/**
	 * <p>Get the name of the DICOM element corresponding to the database table column.</p>
	 *
	 * @param	columnName		the name of the database column
	 * @return				the tag, or null if not known
	 */
	public AttributeTag getAttributeTagFromDatabaseColumnName(String columnName) {
		AttributeTag tag = null;
		String descriptiveName = getDicomNameFromDatabaseColumnName(columnName);
		if (descriptiveName != null) {
			tag = dictionary.getTagFromName(descriptiveName);
		}
		return tag;
	}

	/**
	 * @throws	DicomException
	 */
	private void createTables() throws DicomException {
		InformationEntity ie = rootInformationEntity;
		while (ie != null) {
			createTable(ie,ie != rootInformationEntity);
			ie=getChildTypeForParent(ie);
		}
		createAdditionalIndexes();
	}

	/**
	 * @param	ie
	 * @param	withParentReference
	 * @throws	DicomException
	 */
	private void createTable(InformationEntity ie,boolean withParentReference) throws DicomException {
		String tableName = getTableNameForInformationEntity(ie);
		if (tableName == null) {
			throw new DicomException("Internal error: Cannot get name for table from "+ie);
		}
		try {
			StringBuffer b = new StringBuffer();
			b.append("CREATE CACHED TABLE ");
			//b.append("CREATE TABLE ");
			b.append(tableName);
			b.append(" (");
			extendCreateStatementStringWithMandatoryColumns(b,withParentReference,ie);
			extendCreateStatementStringWithAttributesInDicomDictionary(b,ie);
			extendCreateStatementStringWithAnyExtraAttributes(b,ie);
			extendCreateStatementStringWithDerivedAttributes(b,ie);
			extendCreateStatementStringWithUserColumns(b,ie);
			b.append(")");
			Statement s = databaseConnection.createStatement();
			s.execute(b.toString());	// no ResultSet expected
			s.close();
		} catch (Exception e) {
			throw new DicomException("Cannot create table "+tableName+" in database: "+e);
		}
		// listsOfAttributesByInformationEntity is built later ...
		
		if (withParentReference) {
			try {
				StringBuffer b = new StringBuffer();
				b.append("CREATE INDEX ");
				b.append(tableName);
				b.append("_PREFIDX ON ");
				b.append(tableName);
				b.append(" (");
				b.append(localParentReferenceColumnName);
				b.append(")");
				Statement s = databaseConnection.createStatement();
				s.execute(b.toString());	// no ResultSet expected
				s.close();
			} catch (Exception e) {
				throw new DicomException("Cannot create index of parents for "+tableName+" in database: "+e);
			}
		}
	}

	/**
	 * <p>Create any additional indexes to optimize queries, for example for UIDs.</p>
	 *
	 * <p>Called after creating tables and default indexes.</p>
	 */
	protected void createAdditionalIndexes() throws DicomException {
//System.err.println("DatabaseInformationModel.createAdditionalIndexes():");
		if (additionalIndexMapOfColumnsToTables != null) {
			Iterator i = additionalIndexMapOfColumnsToTables.keySet().iterator();
			while (i.hasNext()) {
				String columnName = (String)(i.next());
				String tableName = (String)(additionalIndexMapOfColumnsToTables.get(columnName));
				try {
					StringBuffer b = new StringBuffer();
					b.append("CREATE INDEX ");
					b.append(tableName);
					b.append("_");
					b.append(columnName);
					b.append("_IDX ON ");
					b.append(tableName);
					b.append(" (");
					b.append(columnName);
					b.append(")");
					Statement s = databaseConnection.createStatement();
					s.execute(b.toString());	// no ResultSet expected
					s.close();
				} catch (Exception e) {
					throw new DicomException("Cannot create index of "+columnName+" for "+tableName+" in database: "+e);
				}
			}
		}
	}

	/**
	 * @param	b
	 * @param	withParentReference
	 * @param	ie
	 */
	private void extendCreateStatementStringWithMandatoryColumns(StringBuffer b,boolean withParentReference,InformationEntity ie) {
		b.append(localPrimaryKeyColumnName);
		b.append(" VARCHAR");
		b.append(" PRIMARY KEY");
		if (withParentReference) {
			b.append(", ");
			b.append(localParentReferenceColumnName);
			b.append(" VARCHAR");
		}
		b.append(",");
		b.append(localRecordInsertionTimeColumnName);
		b.append(" BIGINT");				// not INTEGER, since in hsqldb INTEGER is int, BIGINT is long
		if (ie == InformationEntity.INSTANCE) {
			b.append(",");
			b.append(localFileName);
			b.append(" VARCHAR");
			b.append(",");
			b.append(localFileReferenceTypeColumnName);
			b.append(" CHAR(1)");
		}
	}

	/**
	 * @param	b
	 * @param	ie
	 */
	private void extendCreateStatementStringWithAttributesInDicomDictionary(StringBuffer b,InformationEntity ie) {
		Iterator i = dictionary.getTagIterator();
		while (i.hasNext()) {
			AttributeTag tag = (AttributeTag)i.next();
			if (ie == dictionary.getInformationEntityFromTag(tag)) {
				String tableName = getTableNameForInformationEntity(ie);
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				byte[] vr = dictionary.getValueRepresentationFromTag(tag);
				String columnType = getSQLTypeFromDicomValueRepresentation(vr);
				if (columnName != null && columnType != null) {
//System.err.println(columnName+" = "+tableName+" "+columnType+" "+isAttributeUsedInTable(tableName,columnName));
					b.append(", ");
					b.append(columnName);
					b.append(" ");
					b.append(columnType);
					if (ValueRepresentation.isPersonNameVR(vr)) {
						extendCreateStatementStringWithPersonNameSearchColumns(b,columnName,tableName);
					}
				}
			}
		}
	}

	/**
	 * @param	b
	 * @param	columnName
	 */
	private void extendCreateStatementStringWithPersonNameSearchColumns(StringBuffer b,String columnName,String tableName) {
//System.err.println("extendCreateStatementStringWithPersonNameSearchColumns(): columnName = "+columnName+" tableName = "+tableName);
		{
			String newColumnName = personNameCanonicalColumnNamePrefix + columnName + personNameCanonicalColumnNameSuffix;
			b.append(", ");
			b.append(newColumnName);
			b.append(" ");
			b.append("VARCHAR");

			additionalIndexMapOfColumnsToTables.put(newColumnName,tableName);
		}
		{
			String newColumnName = personNamePhoneticCanonicalColumnNamePrefix + columnName + personNamePhoneticCanonicalColumnNameSuffix;
			b.append(", ");
			b.append(newColumnName);
			b.append(" ");
			b.append("VARCHAR");

			additionalIndexMapOfColumnsToTables.put(newColumnName,tableName);
		}
	}
	
	/**
	 * <p>Extend a SQL CREATE TABLE statement in the process of being constructed with any additional attributes (columns) that the model requires.</p>
	 *
	 * <p>Called when creating the tables for a new database.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.
	 * Defaults to adding no extra columns if not overridden (i.e. it is not abstract).</p>
	 *
	 * <p> For example, there may be a DICOM attribute that is defined to be in a particular information
	 * entity in the dictionary (for example InstanceNumber is at the concatenation level), but for
	 * the convenience of the user of tree and table browsers it may be nice to replicate it into
	 * the tables for lower levels of the information model as well; hence this method might add
	 * InstanceNumber at the instance level. Once created, such attributes will automatically be
	 * included during database inserts. See also
	 * {@link com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel PatientStudySeriesConcatenationInstanceModel}
	 * for example.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a create table statement is being constructed
	 */
	protected void extendCreateStatementStringWithAnyExtraAttributes(StringBuffer b,InformationEntity ie) {}

	/**
	 * <p>Extend a SQL CREATE TABLE statement in the process of being constructed with any derived attributes (columns) that the model requires.</p>
	 *
	 * <p>Called when creating the tables for a new database.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.
	 * Defaults to adding no extra columns if not overridden (i.e. it is not abstract).</p>
	 *
	 * <p> For example, there may be dates and times derived from DICOM attributes.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a create table statement is being constructed
	 */
	protected void extendCreateStatementStringWithDerivedAttributes(StringBuffer b,InformationEntity ie) {}

	/**
	 * <p>Extend a SQL CREATE TABLE statement in the process of being constructed with any user optional columns that the model requires.</p>
	 *
	 * <p>Called when creating the tables for a new database.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.
	 * Defaults to adding four extra columns for each table if not overridden (i.e. it is not abstract).</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a create table statement is being constructed
	 */
	protected void extendCreateStatementStringWithUserColumns(StringBuffer b,InformationEntity ie) {
		b.append(", "); b.append(userColumnName1); b.append(" "); b.append("VARCHAR");
		b.append(", "); b.append(userColumnName2); b.append(" "); b.append("VARCHAR");
		b.append(", "); b.append(userColumnName3); b.append(" "); b.append("VARCHAR");
		b.append(", "); b.append(userColumnName4); b.append(" "); b.append("VARCHAR");
	}

	/**
	 * @param	list
	 * @throws	DicomException
	 */
	private void extendTablesAsNecessary(AttributeList list) throws DicomException {		// doesn't work with Hypersonic ... ALTER command not supported :(
		DicomDictionary dictionary = list.getDictionary();
		Iterator i = list.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			InformationEntity ie = dictionary.getInformationEntityFromTag(tag);
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				String columnName = getDatabaseColumnNameFromDicomTag(tag);
				String columnType = getSQLTypeFromDicomValueRepresentation(a.getVR());	// use actual, not dictionary VR, in case was explicit
				if (columnName != null && columnType != null && !isAttributeUsedInTable(tableName,columnName)) {
//System.err.println(a.toString()+" "+columnName+" = "+tableName+" "+columnType+" "+isAttributeUsedInTable(tableName,columnName));
					try {
						Statement s = databaseConnection.createStatement();
						s.execute("ALTER TABLE " + tableName
							+ " ADD COLUMN " + columnName
							+ " " + columnType
							);	// no ResultSet expected
						s.close();
					} catch (Exception e) {
						throw new DicomException("Cannot add column "+columnName+" to table "+tableName+" in database: "+e);
					}
				}
			}
		}
	}

	/**
	 * @deprecated use  {@link com.pixelmed.database.DatabaseInformationModel#deleteRecord(InformationEntity,String) deleteRecord(InformationEntity,String)} instead
	 */
	public void deleteSelectedRecord(InformationEntity ie,String localPrimaryKeyValue) throws DicomException {
		deleteRecord(ie,localPrimaryKeyValue);
	}

	/**
	 * <p>Delete a database record (a particular instance of an information entity).</p>
	 *
	 * <p>For example, for the study entity, this would delete a particular study.</p>
	 *
	 * <p>Does NOT delete its children, if any.</p>
	 *
	 * <p>Does NOT delete any referenced files, if any.</p>
	 *
	 * @param	ie						the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that corresponds to the table containing the record to be deleted
	 * @param	localPrimaryKeyValue	primary key of the record
	 * @throws	DicomException	thrown if there are problems executing the database statement
	 */
	public void deleteRecord(InformationEntity ie,String localPrimaryKeyValue) throws DicomException {
//System.err.println("DatabaseInformationModel.deleteRecord(): ie = "+ie);
//System.err.println("DatabaseInformationModel.deleteRecord(): localPrimaryKeyValue = "+localPrimaryKeyValue);
		if (ie != null && localPrimaryKeyValue != null && localPrimaryKeyValue.length() > 0) {
			try {
				String tableName = getTableNameForInformationEntity(ie);
//System.err.println("DatabaseInformationModel.deleteRecord(): tableName = "+tableName);
				StringBuffer b = new StringBuffer();
				b.append("DELETE FROM ");
				b.append(tableName);
				b.append(" WHERE ");
				b.append(localPrimaryKeyColumnName);
				b.append(" = \'");
				b.append(localPrimaryKeyValue);
				b.append("\'");
				Statement s = databaseConnection.createStatement();
				String ss = b.toString();
//System.err.println("DatabaseInformationModel.deleteRecord(): Statement to execute = "+ss);
				s.execute(ss);	// no ResultSet expected
				s.close();
			}
			catch (Exception e) {
				slf4jlogger.error("Rethrowing database exception during deleteRecord as DicomException",e);
				throw new DicomException("Cannot perform deletion: "+e);
			}
		}
	}
	
	/**
	 * <p>Insert a DICOM composite object, and the relevant attributes of all the entities it contains, into the database.</p>
	 *
	 * <p>Such a composite object may contain information about the patient, study, series and
	 * instance and so on, and for each of these entities the appropriate records will be
	 * created in the appropriate tables</p>
	 *
	 * <p>If records for any entities already exist (as they will often do when inserting multiple
	 * objects for the same patient or study, for example), they are matched based on the unique key for
	 * the appropriate level. They are not replaced and they are not updated, even if some
	 * of the attributes for that entity (other than the unique key) are different or
	 * additional. Even at the instance level (e.g. if an object with the same <code>SOPInstanceUID</code>
	 * is received), the first record will not be overwritten. Whether or not the application calling this method
	 * will have overwritten the corresponding file in the supplied argument or not is outside the
	 * scope of this class.</p>
	 *
	 * <p>The information is taken from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList},
	 * which is presumed to have already been read from a file or obtained through some other means. The supplied
	 * filename is only used to fill in the appropriate instance level attribute.</p>
	 *
	 * <p>The file reference type is left empty since unknown.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.database.DatabaseInformationModel#insertObject(AttributeList,String,String) insertObject()} instead
	 *
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	fileName	the name of a file where the object is stored and from whence it may later be read
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	public void insertObject(AttributeList list,String fileName) throws DicomException {
		insertObject(list,fileName,""/*fileReferenceType*/);
	}
	
	/**
	 * <p>Insert a DICOM composite object, and the relevant attributes of all the entities it contains, into the database.</p>
	 *
	 * <p>Such a composite object may contain information about the patient, study, series and
	 * instance and so on, and for each of these entities the appropriate records will be
	 * created in the appropriate tables</p>
	 *
	 * <p>If records for any entities already exist (as they will often do when inserting multiple
	 * objects for the same patient or study, for example), they are matched based on the unique key for
	 * the appropriate level. They are not replaced and they are not updated, even if some
	 * of the attributes for that entity (other than the unique key) are different or
	 * additional. Even at the instance level (e.g. if an object with the same <code>SOPInstanceUID</code>
	 * is received), the first record will not be overwritten. Whether or not the application calling this method
	 * will have overwritten the corresponding file in the supplied argument or not is outside the
	 * scope of this class.</p>
	 *
	 * <p>The information is taken from the supplied {@link com.pixelmed.dicom.AttributeList AttributeList},
	 * which is presumed to have already been read from a file or obtained through some other means. The supplied
	 * filename is only used to fill in the appropriate instance level attribute.</p>
	 *
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	fileName	the name of a file where the object is stored and from whence it may later be read
	 * @param	fileReferenceType	"C" for copied (i.e., delete on purge), "R" for referenced (i.e., do not delete on purge)
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	public void insertObject(AttributeList list,String fileName,String fileReferenceType) throws DicomException {
//System.err.println("DatabaseInformationModel.insertObject(): fileName="+fileName);
		// iterate through information entities, extracting matching keys, checking for a match, inserting new if not ...
		try {
			InformationEntity ie = rootInformationEntity;
			String localParentReference = null;
			while (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT ");
				b.append(localPrimaryKeyColumnName);
				b.append(" FROM ");
				b.append(tableName);
				b.append(" WHERE ");
				if (ie != rootInformationEntity) {
					b.append(tableName);
					b.append(".");
					b.append(localParentReferenceColumnName);
					//b.append(" LIKE \'");
					b.append(" = \'");
					b.append(localParentReference);
					b.append("\'");
				}
				extendStatementStringWithMatchingAttributesForSelectedInformationEntity(b,list,ie);

				Statement s = databaseConnection.createStatement();
				String ss = b.toString();
//System.err.println("DatabaseInformationModel.insertObject(): Statement to execute = "+ss);
				ResultSet r = s.executeQuery(ss);

				String entityPrimaryKey = null;
				int count = 0;
				while (r.next()) {
					entityPrimaryKey=r.getString(localPrimaryKeyColumnName).trim();		// since CHAR not VARCHAR, returns trailing spaces :(
					++count; 
				}
//System.err.println("DatabaseInformationModel.insertObject(): ie="+ie+" count="+count+" entityPrimaryKey="+entityPrimaryKey);
				if (count != 1 || entityPrimaryKey == null) {	// too few or too many ... make a new entry ...
//System.err.println("DatabaseInformationModel.insertObject(): Inserting new row in "+tableName);
					b = new StringBuffer();
					b.append("INSERT INTO ");
					b.append(tableName);
					b.append(" (");
					b.append(localPrimaryKeyColumnName);
					if (ie != rootInformationEntity) {
						b.append(",");
						b.append(localParentReferenceColumnName);
					}
					b.append(",");
					b.append(localRecordInsertionTimeColumnName);
					extendInsertStatementStringWithAttributeNamesForSelectedInformationEntity(b,list,ie);
					extendInsertStatementStringWithDerivedAttributeNamesForSelectedInformationEntity(b,list,ie);
					extendInsertStatementStringWithPersonNameSearchColumnsForSelectedInformationEntity(b,list,ie);
					b.append(") VALUES (\'");
					entityPrimaryKey=createPrimaryKeyForSelectedInformationEntity(ie);
					b.append(entityPrimaryKey);
					b.append("\'");
					if (ie != rootInformationEntity) {
						b.append(",\'");
//System.err.println("DatabaseInformationModel.insertObject(): localParentReference = <"+localParentReference+">");
//System.err.println("DatabaseInformationModel.insertObject(): localParentReference.length() = "+localParentReference.length());
						b.append(localParentReference);
						b.append("\'");
					}
					b.append(",");
					b.append(Long.toString(System.currentTimeMillis()));	// no quotes, since INTEGER
					extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(b,list,ie,fileName,fileReferenceType);
//System.err.println("DatabaseInformationModel.insertObject(): After extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity = "+b.toString());
					extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(b,list,ie);
//System.err.println("DatabaseInformationModel.insertObject(): After extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity = "+b.toString());
					extendInsertStatementStringWithPersonNameSearchValuesForSelectedInformationEntity(b,list,ie);
					b.append(")");
					ss = b.toString();
//System.err.println("DatabaseInformationModel.insertObject(): Statement to execute = "+ss);
					s.execute(ss);	// no ResultSet expected
				}

				s.close();
//System.err.println("DatabaseInformationModel.insertObject(): Done "+tableName+" entityPrimaryKey="+entityPrimaryKey+" localParentReference="+localParentReference);
				localParentReference=entityPrimaryKey;
				ie=getChildTypeForParent(ie,list);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("Rethrowing database exception during insertObject as DicomException", e);
			throw new DicomException("Cannot perform selection: "+e);
		}
	}

	/**
	 * @param	b
	 */
	private void extendStatementStringWithListOfAllTables(StringBuffer b) {
		InformationEntity ie = rootInformationEntity;
		while (ie != null) {
			if (ie != rootInformationEntity) b.append(",");
			String tableName = getTableNameForInformationEntity(ie);
			b.append(tableName);
			ie=getChildTypeForParent(ie);
		}
	}

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the additional search columns derived
	 * from person name attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a select statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithPersonNameSearchColumnsForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;
	
	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the additional search columns derived
	 * from person name attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a select statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithPersonNameSearchValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;

	/**
	 * <p>Extend a SQL SELECT statement in the process of being constructed with matching clauses for the unique keys of the entity and all its parents.</p>
	 *
	 * <p>For example, a model might specify the unique key for the patient to be <code>PatientID</code>,
	 * the study to be <code>StudyInstanceUID</code>, the series to be <code>SeriesInstanceUID</code> and so on. A
	 * match requested at the series level would then require <code>PatientID</code>, <code>StudyInstanceUID</code>
	 * and <code>SeriesInstanceUID</code> to match what was in the supplied {@link com.pixelmed.dicom.AttributeList AttributeList}</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a select statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendStatementStringWithMatchingAttributesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithAttributeNamesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * <p>The file reference type is left empty since unknown.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.database.DatabaseInformationModel#extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer,AttributeList,InformationEntity,String,String) extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity()} instead
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @param	fileName	the local filename, which may be non-null for <code>INSTANCE</code> level insertions
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie,String fileName) throws DicomException;

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @param	fileName	the local filename, which may be non-null for <code>INSTANCE</code> level insertions
	 * @param	fileReferenceType	"C" for copied (i.e., delete on purge), "R" for referenced (i.e., do not delete on purge)
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie,String fileName,String fileReferenceType) throws DicomException;
	
	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the names of the derived attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithDerivedAttributeNamesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;

	/**
	 * <p>Extend a SQL INSERT statement in the process of being constructed with the values of the derived attributes in the instance for the entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * <p>Specific to each concrete information model extending {@link DatabaseInformationModel DatabaseInformationModel}.</p>
	 *
	 * @param	b		the statement being constructed
	 * @param	list		the DICOM attributes of a composite object, containing the attributes describing this instance of the entity
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which an insert statement is being constructed
	 * @throws	DicomException	thrown if there are problems extracting the DICOM attributes
	 */
	protected abstract void extendInsertStatementStringWithDerivedAttributeValuesForSelectedInformationEntity(StringBuffer b,AttributeList list,InformationEntity ie) throws DicomException;
	
	/**
	 * <p>Create a new unique key which may be used to identify a new instance of an entity.</p>
	 *
	 * <p>Called when inserting a new record for an instance of the entity.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity} for which a key is requested
	 * @return		string value of a unique key
	 */
	protected abstract String createPrimaryKeyForSelectedInformationEntity(InformationEntity ie);

	/**
	 * @throws	SQLException
	 */
	private void primeListsOfAttributesByInformationEntityFromExistingMetaData() throws SQLException {
		DatabaseMetaData meta = databaseConnection.getMetaData();
		ResultSet columns = meta.getColumns(null, null, null, null);
		while (columns.next()) {
			String tableName  = columns.getString(3);
			String columnName = columns.getString(4);
			//String type = columns.getString(6);
//System.err.println("DatabaseInformationModel.primeListsOfAttributesByInformationEntityFromExistingMetaData(): tableName="+tableName+" columnName="+columnName);
			if (tableName != null && columnName != null && isInformationEntityInModel(tableName)) {
//System.err.println("DatabaseInformationModel.primeListsOfAttributesByInformationEntityFromExistingMetaData(): non-null and tableName is in the model");
				LinkedList listOfAttributes = (LinkedList)listsOfAttributesByInformationEntity.get(tableName);
				if (listOfAttributes == null) {
					listOfAttributes = new LinkedList();
					listsOfAttributesByInformationEntity.put(tableName,listOfAttributes);	// NB. column name will be all UPPERCASE
				}
//System.err.println("DatabaseInformationModel.primeListsOfAttributesByInformationEntityFromExistingMetaData(): adding to table "+tableName+" "+columnName);
				listOfAttributes.add(columnName);
			}
		}
		columns.close();
	}

	/**
	 * <p>Is the specified attribute (column) recorded in the specified entity?</p>
	 *
	 * @param	ie		the {@link com.pixelmed.dicom.InformationEntity InformationEntity}
	 * @param	columnName	the string name of the attribute (the column name) (case insensitive)
	 * @return			true if the attribute is used in the table
	 */
	boolean isAttributeUsedInTable(InformationEntity ie,String columnName) {
		return ie == null ? false : isAttributeUsedInTable(getTableNameForInformationEntity(ie),columnName);
	}

	/**
	 * <p>Is the specified attribute (column) recorded in the specified table?</p>
	 *
	 * @param	tableName	the string name of the table (case insensitive)
	 * @param	columnName	the string name of the attribute (the column name) (case insensitive)
	 * @return			true if the attribute is used in the table
	 */
	boolean isAttributeUsedInTable(String tableName,String columnName) {
		if (listsOfAttributesByInformationEntity != null) {
			LinkedList listOfAttributes = (LinkedList)listsOfAttributesByInformationEntity.get(tableName.toUpperCase(java.util.Locale.US));
			if (listOfAttributes != null) {
				if (listOfAttributes.contains(columnName.toUpperCase(java.util.Locale.US))) {
//System.err.println("DatabaseInformationModel.isAttributeUsedInTable(String,String): "+tableName+" contains "+columnName);
					return true;	// NB. depends on all identical strings being same object
				}
				else {
//System.err.println("DatabaseInformationModel.isAttributeUsedInTable(String,String): "+tableName+" does not contain "+columnName);
				}
			}
		}
		return false;
	}

	/**
	 * <p>For a particular instance of an information entity, update the record in the database table with a new value for the specified attribute (column).</p>
	 *
	 * <p>For example, for the study entity, this would update an attribute of a particular study, for example the <code>StudyID</code> attribute.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the record to be updated
	 * @param	localPrimaryKeyValue	the string value of the unique key which identifies the instance of the entity (not including wildcards)
	 * @param	key			the string name of the attribute (column) to be set (updated)
	 * @param	value			the string value to set
	 * @throws	DicomException		thrown if the update fails
	 */
	public void updateSelectedRecord(InformationEntity ie,String localPrimaryKeyValue,String key,String value) throws DicomException {
//System.err.println("DatabaseInformationModel.updateSelectedRecord(): "+ie+" "+localPrimaryKeyValue+" "+key+" "+value);
		try {
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("UPDATE ");
				b.append(tableName);
				if (key != null) {
					b.append(" SET ");
					b.append(key);
					b.append(" = \'");
					b.append(value);
					b.append("\'");
				}
				if (localPrimaryKeyValue != null) {
					b.append(" WHERE ");
					b.append(localPrimaryKeyColumnName);
					//b.append(" LIKE \'");
					b.append(" = \'");
					b.append(localPrimaryKeyValue);
					b.append("\'");
				}
				b.append(";");
				Statement s = databaseConnection.createStatement();
				s.execute(b.toString());	// no ResultSet expected
				s.close();
			}
		} catch (Exception e) {
            slf4jlogger.error("Rethrowing database exception during updateSelectedRecord as DicomException", e);
			throw new DicomException("Cannot perform update: "+e);
		}
	}

	/**
	 * <p>For a particular instance of an information entity, get the values of all the columns in the entity's database table.</p>
	 *
	 * <p>For example, for the study entity, this would return the attributes for a particular study.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the record to be returned
	 * @param	localPrimaryKeyValue	the string value of the unique key which identifies the instance of the entity (not including wildcards)
	 * @return				a {@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public Map findAllAttributeValuesForSelectedRecord(InformationEntity ie,String localPrimaryKeyValue) throws DicomException {
		TreeMap map = new TreeMap();
		try {
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT * FROM ");
				b.append(tableName);
				if (localPrimaryKeyValue != null) {
					b.append(" WHERE ");
					b.append(localPrimaryKeyColumnName);
					//b.append(" LIKE \'");
					b.append(" = \'");
					b.append(localPrimaryKeyValue);
					b.append("\'");
				}
				b.append(";");
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(b.toString());
				ResultSetMetaData md = r.getMetaData();
				int numberOfColumns = md.getColumnCount();
				if (r.next()) {							// there should be exactly one
					for (int i=1; i<=numberOfColumns; ++i) {
						String key = md.getColumnName(i);	// will be upper case
						String value = r.getString(i);
//System.err.println("findAllAttributeValuesForSelectedRecord: ["+i+"] key = "+key+" value = "+value);
						map.put(key,value);
					}
				}
				s.close();
			}
		} catch (Exception e) {
			slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
		return map;
	}

	/**
	 * <p>For a particular instance of an information entity, get the value of the selected column in the entity's database table.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the record to be returned
	 * @param	localPrimaryKeyValue	the string value of the unique key which identifies the instance of the entity (not including wildcards)
	 * @param	key						the name of the attribute to be returned
	 * @return				a {@link java.lang.String String} value, or an empty string if not found
	 * @throws	DicomException		thrown if the query fails
	 */
	public String findSelectedAttributeValuesForSelectedRecord(InformationEntity ie,String localPrimaryKeyValue,String key) throws DicomException {
		String value = "";
		try {
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT ");
				b.append(key);
				b.append(" FROM ");
				b.append(tableName);
				if (localPrimaryKeyValue != null) {
					b.append(" WHERE ");
					b.append(localPrimaryKeyColumnName);
					//b.append(" LIKE \'");
					b.append(" = \'");
					b.append(localPrimaryKeyValue);
					b.append("\'");
				}
				b.append(";");
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(b.toString());
				ResultSetMetaData md = r.getMetaData();
				int numberOfColumns = md.getColumnCount();
				if (r.next()) {							// there should be exactly one
					if (numberOfColumns == 1 && md.getColumnName(1).equals(key.toUpperCase())) {	// returned Column Name will be upper case
						value = r.getString(1);
//System.err.println("findSelectedAttributeValuesForSelectedRecord: key = "+key+" value = "+value);
					}
				}
				s.close();
			}
		} catch (Exception e) {
			slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
		return value;
	}

	/**
	 * <p>For all records of an information entity, get the values of all the columns in the entity's database table.</p>
	 *
	 * <p>For example, for the study entity, this would return the attributes for all the studies in the database.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the records to be returned
	 * @return				an {@link java.util.ArrayList ArrayList} of records, each value of which is a
	 *					{@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public ArrayList findAllAttributeValuesForAllRecordsForThisInformationEntity(InformationEntity ie) throws DicomException {
//long startTime=System.currentTimeMillis();
		ArrayList recordsAsMapsOfStrings = new ArrayList();
		try {
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT * FROM ");
				b.append(tableName);
				b.append(";");
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(b.toString());
				ResultSetMetaData md = r.getMetaData();
				int numberOfColumns = md.getColumnCount();
				while (r.next()) {
					TreeMap map = new TreeMap();
					for (int i=1; i<=numberOfColumns; ++i) {
						String key = md.getColumnName(i);	// will be upper case
						String value = r.getString(i);
//System.err.println("findAllAttributeValuesForAllRecordsForThisInformationEntity: ["+i+"] key = "+key+" value = "+value);
						map.put(key,value);
					}
					recordsAsMapsOfStrings.add(map);
				}
				s.close();
			}
		} catch (Exception e) {
            slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
//System.err.println("Select all values for all records of "+ie.toString()+" time "+(System.currentTimeMillis()-startTime)+" milliseconds");
		return recordsAsMapsOfStrings;
	}
	
	/**
	 * <p>For all records of an information entity matching the specified UID, get the values of all the columns in the entity's database table.</p>
	 *
	 * <p>For example, for the study entity, this would return the attributes for all the studies with the specified UID in the database.</p>
	 *
	 * <p>For the instance entity, this would return the attributes for all the instances with the specified UID in the database.</p>
	 *
	 * <p>In an ideal world, this method should only ever return one record, since the DICOM model is predicated on UIDs being unique !</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the records to be returned
	 * @param	uid			the {@link java.lang.String String} UID of the records to be returned
	 * @return				an {@link java.util.ArrayList ArrayList} of records, each value of which is a
	 *					{@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public ArrayList findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedUID(InformationEntity ie,String uid) throws DicomException {
		return findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(ie,getUIDColumnNameForInformationEntity(ie),uid);
	}
	
	/**
	 * <p>For all records of an information entity matching the specified key value, get the values of all the columns in the entity's database table.</p>
	 *
	 * <p>For example, for the study entity, this would return the attributes for all the studies with the specified key value in the database.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the records to be returned
	 * @param	keyName		the {@link java.lang.String String} name of the key to be matched
	 * @param	keyValue	the {@link java.lang.String String} value of the key to be matched
	 * @return				an {@link java.util.ArrayList ArrayList} of records, each value of which is a
	 *					{@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public ArrayList findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(InformationEntity ie,String keyName,String keyValue) throws DicomException {
//long startTime=System.currentTimeMillis();
		ArrayList recordsAsMapsOfStrings = new ArrayList();
		try {
			if (ie != null && keyName != null && keyValue != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT * FROM ");
				b.append(tableName);
				b.append(" WHERE ");
				b.append(keyName);
				b.append(" = ");
				b.append("\'");
				b.append(keyValue);
				b.append("\'");
				b.append(";");
				String ss = b.toString();
//System.err.println("findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(): statement to execute = "+ss);
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(ss);
				ResultSetMetaData md = r.getMetaData();
				int numberOfColumns = md.getColumnCount();
				while (r.next()) {
					TreeMap map = new TreeMap();
					for (int i=1; i<=numberOfColumns; ++i) {
						String key = md.getColumnName(i);	// will be upper case
						String value = r.getString(i);
//System.err.println("findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedKeyValue(): ["+i+"] key = "+key+" value = "+value);
						map.put(key,value);
					}
					recordsAsMapsOfStrings.add(map);
				}
				s.close();
			}
		} catch (Exception e) {
			slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
//System.err.println("Select all values for all records of "+ie.toString()+" time "+(System.currentTimeMillis()-startTime)+" milliseconds");
		return recordsAsMapsOfStrings;
	}
	
	private HashMap mapOfInformationEntitiesToColumnNames;
	
	private final String[] getArrayOfColumnNamesForSpecifiedInformationEntity(InformationEntity ie,ResultSet r) throws java.sql.SQLException {
		if (mapOfInformationEntitiesToColumnNames == null) {
			mapOfInformationEntitiesToColumnNames = new HashMap();
		}
		String[] columnNames = (String[])(mapOfInformationEntitiesToColumnNames.get(ie));
		if (columnNames == null) {
//System.err.println("getArrayOfColumnNamesForSpecifiedInformationEntity() "+ie.toString()+" caching metadata");
			ResultSetMetaData md = r.getMetaData();
			int numberOfColumns = md.getColumnCount();
			ArrayList list = new ArrayList();
			for (int i=1; i<=numberOfColumns; ++i) {
				String columName = md.getColumnName(i);	// will be upper case
				list.add(columName);
			}
			columnNames=new String[numberOfColumns];
			columnNames=(String[])(list.toArray(columnNames));
			mapOfInformationEntitiesToColumnNames.put(ie,columnNames);
		}
		return columnNames;
	}

	/**
	 * <p>For all records of an information entity with the specified parent, get the values of all the columns in the entity's database table.</p>
	 *
	 * <p>For example, for the series entity, this would return the attributes for all the series of a particular study (parent).</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the records to be returned
	 * @param	localParentReference	the string value of the unique key which identifies the instance of the parent entity (not including wildcards)
	 * @return				an {@link java.util.ArrayList ArrayList} of records, each value of which is a
	 *					{@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public ArrayList findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(InformationEntity ie,String localParentReference) throws DicomException {
//long startTime=System.currentTimeMillis();
		ArrayList recordsAsMapsOfStrings = new ArrayList();
		try {
			if (ie != null) {
				String tableName = getTableNameForInformationEntity(ie);
				StringBuffer b = new StringBuffer();
				b.append("SELECT * FROM ");
				b.append(tableName);
				if (ie != rootInformationEntity && localParentReference != null) {
					b.append(" WHERE ");
					b.append(localParentReferenceColumnName);
					//b.append(" LIKE \'");
					b.append(" = \'");
					b.append(localParentReference);
					b.append("\'");
				}
				b.append(";");
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(b.toString());
//System.err.println("Select all values for all records of "+ie.toString()+" with specified parent select only time "+(System.currentTimeMillis()-startTime)+" milliseconds");
				String[] columnNames = getArrayOfColumnNamesForSpecifiedInformationEntity(ie,r);
//System.err.println("Select all values for all records of "+ie.toString()+" with specified parent get column names only time "+(System.currentTimeMillis()-startTime)+" milliseconds");
				int numberOfColumns = columnNames.length;
//long timeInNextResult = 0;
//long timeInMappingColumns = 0;
//long resultSetIteratorStartTime = System.currentTimeMillis();
				while (r.next()) {
//long resultSetIteratorEndTime = System.currentTimeMillis();
//timeInNextResult+=(resultSetIteratorEndTime-resultSetIteratorStartTime);
					TreeMap map = new TreeMap();
//long mappingColumnsStartTime = System.currentTimeMillis();
					for (int i=1; i<=numberOfColumns; ++i) {
						String key = columnNames[i-1];
						String value = r.getString(i);
//System.err.println("findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent: ["+i+"] key = "+key+" value = "+value);
						map.put(key,value);
					}
//long mappingColumnsEndTime = System.currentTimeMillis();
//timeInMappingColumns+=(mappingColumnsEndTime-mappingColumnsStartTime);
					recordsAsMapsOfStrings.add(map);
//resultSetIteratorStartTime = System.currentTimeMillis();
				}
//System.err.println("Select all values for all records of "+ie.toString()+" with specified parent select timeInNextResult "+timeInNextResult+" milliseconds");
//System.err.println("Select all values for all records of "+ie.toString()+" with specified parent select timeInMappingColumns "+timeInMappingColumns+" milliseconds");
				s.close();
			}
		} catch (Exception e) {
            slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
//System.err.println("Select all values for all records of "+ie.toString()+" with specified parent time "+(System.currentTimeMillis()-startTime)+" milliseconds");
		return recordsAsMapsOfStrings;
	}

	/**
	 * <p>For all records of an information entity with all parents matching a particular attribute value, get the values of all attributes.</p>
	 *
	 * <p>For example, for the instance entity, this could return the instance local file name for all the series (parents) with a particular frame of reference UID.</p>
	 *
	 * @param	ieWanted		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the record level of the attribute to be returned
	 * @param	ieParent		the {@link com.pixelmed.dicom.InformationEntity InformationEntity} of the record level of the parent to be matched
	 * @param	parentMatchingAttribute	the string name of the attribute of the parent whose value is to be matched
	 * @param	parentMatchingValue	the string value of the attribute of the parent to be matched (not including wildcards)
	 * @return				an {@link java.util.ArrayList ArrayList} of records, each value of which is a
	 *					{@link java.util.TreeMap TreeMap} of {@link java.lang.String String} values indexed by {@link java.lang.String String} upper case column names
	 * @throws	DicomException		thrown if the query fails
	 */
	public ArrayList findAllAttributeValuesForAllRecordsForThisInformationEntityWithMatchingParent(
			InformationEntity ieWanted,InformationEntity ieParent,String parentMatchingAttribute,String parentMatchingValue) throws DicomException {
//long startTime=System.currentTimeMillis();
		ArrayList recordsAsMapsOfStrings = new ArrayList();
		try {
			if (ieWanted != null && ieParent != null) {
				String wantedTableName = getTableNameForInformationEntity(ieWanted);
				String parentTableName = getTableNameForInformationEntity(ieParent);
				StringBuffer b = new StringBuffer();
				b.append("SELECT * FROM ");
				b.append(wantedTableName);
				b.append(",");
				b.append(parentTableName);
				b.append(" WHERE ");
				b.append(wantedTableName);
				b.append(".");
				b.append(localParentReferenceColumnName);
				b.append(" = ");
				b.append(parentTableName);
				b.append(".");
				b.append(localPrimaryKeyColumnName);
				b.append(" AND ");
				b.append(parentTableName);
				b.append(".");
				b.append(parentMatchingAttribute);
				//b.append(" LIKE \'");
				b.append(" = \'");
				b.append(parentMatchingValue);
				b.append("\'");
				b.append(";");
				String str = b.toString();
//System.err.println("findSelectedAttributeValueForAllRecordsForThisInformationEntityWithMatchingParent: "+str);
				Statement s = databaseConnection.createStatement();
				ResultSet r = s.executeQuery(str);
				ResultSetMetaData md = r.getMetaData();
				int numberOfColumns = md.getColumnCount();
				while (r.next()) {
					TreeMap map = new TreeMap();
					for (int i=1; i<=numberOfColumns; ++i) {
						String key = md.getColumnName(i);	// will be upper case
						String value = r.getString(i);
//System.err.println("findSelectedAttributeValueForAllRecordsForThisInformationEntityWithMatchingParent: ["+i+"] key = "+key+" value = "+value);
						map.put(key,value);
					}
					recordsAsMapsOfStrings.add(map);
				}
				s.close();
			}
		} catch (Exception e) {
            slf4jlogger.error("Rethrowing database exception during selection as DicomException",e);
			throw new DicomException("Cannot perform selection: "+e);
		}
//System.err.println("Select all values for all records of "+ieWanted.toString()+" with matching parent time "+(System.currentTimeMillis()-startTime)+" milliseconds");
		return recordsAsMapsOfStrings;
	}

	/**
	 * <p>Get the table name for an information entity.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the parent, such as a patient,  study, etc.
	 * @return				the upper case name of the table as used in the database
	 */
	public static String getTableNameForInformationEntity(InformationEntity ie) {
		return ie.toString().toUpperCase(java.util.Locale.US);
	}

	/**
	 * <p>For an information entity (regardless of a particular instance), find the next information entity lower down in the information model hierarchy in the general case.</p>
	 *
	 * <p>For a patient, this might be a study.</p>
	 * <p>For a series, this might be a concatenation
	 * or an instance, depending on the information model. 
	 *
	 * <p>This method essentially returns the most complex model possible and is used when building the database table schema.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the parent, such as a patient,  study, etc.
	 * @return				the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the child
	 */
	public abstract InformationEntity getChildTypeForParent(InformationEntity ie);
	
	/**
	 * <p>For a particular instance of an information entity, find the next information entity lower down in the information model hierarchy.</p>
	 *
	 * <p>For a patient, this might be a study. For a series, this might be a concatenation
	 * or an instance, depending on both the information model and whether concatenations are to be considered in the model. 
	 *
	 * <p>This method may return a simpler view than the more general method, and is used when traversing the database tables.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the parent, such as a
	 *					patient,  study, etc.
	 * @param	concatenation		true if concatenations are to be considered in the model
	 * @return				the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the child
	 */
	public abstract InformationEntity getChildTypeForParent(InformationEntity ie,boolean concatenation);
	
	/**
	 * <p>For a particular instance of an information entity, find the next information entity lower down in the information model hierarchy.</p>
	 *
	 * <p>For a patient, this might be a study. For a series, this might be a concatenation
	 * or an instance, depending on both the information model and the contents of the instance itself. 
	 *
	 * <p>This method may return a simpler view than the more general method, and is used when traversing the database tables.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the parent, such as a
	 *					patient,  study, etc.
	 * @param	list			the {@link com.pixelmed.dicom.AttributeList AttributeList} that are the contents the instance
	 * @return				the {@link com.pixelmed.dicom.InformationEntity InformationEntity} that is the child
	 */
	public abstract InformationEntity getChildTypeForParent(InformationEntity ie,AttributeList list);

	/**
	 * <p>For a particular instance of an information entity, find a descriptive name for the entity suitable for rendering.</p>
	 *
	 * <p>For a patient, this might be the name <code>Patient</code>.
	 * For an instance, this will depend on the SOPClassUID, and might be an <code>Image</code>, a <code>Waveform</code>, etc.</p>
	 *
	 * @param	ie			the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *					patient,  study, etc.
	 * @param	returnedAttributes	the attributes from the selected row of the table for this instance of the entity
	 * @return				a human-readable string name of the entity
	 */
	public abstract String getNametoDescribeThisInstanceOfInformationEntity(InformationEntity ie,Map returnedAttributes);
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the first of three descriptive attributes for the entity.</p>
	 *
	 * <p>For example, for an instance (e.g. an image), this might be the <code>InstanceNumber</code>. 
	 * For a patient, this might be the <code>PatientName</code>.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient, study, etc.
	 * @return		the upper case string name of the column, or <code>null</code> if there is no such column
	 */
	public abstract String getDescriptiveColumnName(InformationEntity ie);
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the second of three descriptive attributes for the entity.</p>
	 *
	 * <p>For example, for an instance (e.g. an image), this might be the <code>InConcatenationNumber</code>. 
	 * Frequently null for other entities.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient, study, etc.
	 * @return		the upper case string name of the column, or <code>null</code> if there is no such column
	 */
	public abstract String getOtherDescriptiveColumnName(InformationEntity ie);

	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the third of three descriptive attributes for the entity.</p>
	 *
	 * <p>For example, for an instance (e.g. an image), this might be the <code>ImageComments</code>. 
	 * For a patient, this might be the <code>PatientID</code>.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient, study, etc.
	 * @return		the upper case string name of the column, or <code>null</code> if there is no such column
	 */
	public abstract String getOtherOtherDescriptiveColumnName(InformationEntity ie);
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the DICOM UID for the entity.</p>
	 *
	 * <p>For example, for an instance (e.g. an image), this might be the name of the column corresponding to the SOP Instance UID.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			study, series, instance, etc.
	 * @return		the upper case string name of the column, or <code>null</code> if there is no such column
	 */
	public abstract String getUIDColumnNameForInformationEntity(InformationEntity ie);
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the primary key of an instance of the entity.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient, study, etc.
	 * @return		the upper case string name of the column	
	 */
	public String getLocalPrimaryKeyColumnName(InformationEntity ie) { return localPrimaryKeyColumnName; }
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the local file name of a stored object.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			instance (e.g. an image)
	 * @return		the upper case string name of the column	
	 */
	public String getLocalFileNameColumnName(InformationEntity ie) { return localFileName; }
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the local file name of a stored object.</p>
	 *
	 * @deprecated use  {@link com.pixelmed.database.DatabaseInformationModel#getLocalFileNameColumnName(InformationEntity) getLocalFileNameColumnName()} instead
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			instance (e.g. an image)
	 * @return		the upper case string name of the column	
	 */
	public String localFileNameColumnName(InformationEntity ie) { return getLocalFileNameColumnName(ie); }
	
	
	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the reference type of a stored object.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			instance (e.g. an image)
	 * @return		the upper case string name of the column	
	 */
	public String getLocalFileReferenceTypeColumnName(InformationEntity ie) { return localFileReferenceTypeColumnName; }

	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the reference to an instance of the entity's parent.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient,  study, etc.
	 * @return		the upper case string name of the column	
	 */
	public String getLocalParentReferenceColumnName(InformationEntity ie) { return localParentReferenceColumnName; }

	/**
	 * <p>For a particular information entity, find the name of the column in the entity's database table containing the record insertion time recorded as the value returned by {@link java.lang.System#currentTimeMillis() System.currentTimeMillis()}.</p>
	 *
	 * @param	ie	the {@link com.pixelmed.dicom.InformationEntity InformationEntity}, such as a
	 *			patient,  study, etc.
	 * @return		the upper case string name of the column	
	 */
	public String getLocalRecordInsertionTimeColumnName(InformationEntity ie) { return localRecordInsertionTimeColumnName; }

	/**
	 * <p>Given a DICOM Value Representation, determine the appropriate corresponding SQL type to use.</p>
	 *
	 * <p>For example, the DICOM AE VR should be represented as a CHAR(16), an SS as an INTEGER, and so on.</p>
	 *
	 * @param	vr	the 2 letter DICOM Value Representation as an array of two ASCII bytes.
	 * @return		the string representing the SQL type.
	 */
	public static String getSQLTypeFromDicomValueRepresentation(byte[] vr) {
		String s;
		if (ValueRepresentation.isApplicationEntityVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isAgeStringVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isCodeStringVR(vr)) {
			//s="VARCHAR";
			s="VARCHAR";	// 16 byte length is frequently violated :(
		}
		else if (ValueRepresentation.isDateVR(vr)) {
			//s="DATE";
			s="VARCHAR";		// should be 8, but in case old form with colons
		}
		else if (ValueRepresentation.isDateTimeVR(vr)) {
			//s="DATETIME";
			s="VARCHAR";	// not just yyyymmddhhmmss; may have fractional, timezone
		}
		else if (ValueRepresentation.isDecimalStringVR(vr)) {
			//s="REAL";	// this fails if VM > 1, e.g. ImagePositionPatient
			s="VARCHAR";
		}
		else if (ValueRepresentation.isFloatDoubleVR(vr)) {
			s="REAL";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isFloatSingleVR(vr)) {
			s="REAL";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isIntegerStringVR(vr)) {
			s="INTEGER";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isLongStringVR(vr)) {
			s="VARCHAR";	// 64 each value
		}
		else if (ValueRepresentation.isLongTextVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isOtherByteVR(vr)) {
			s=null;
		}
		else if (ValueRepresentation.isOtherWordVR(vr)) {
			s=null;
		}
		else if (ValueRepresentation.isOtherUnspecifiedVR(vr)) {
			s=null;
		}
		else if (ValueRepresentation.isPersonNameVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isSequenceVR(vr)) {
			s=null;
		}
		else if (ValueRepresentation.isShortStringVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isSignedLongVR(vr)) {
			s="INTEGER";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isSignedShortVR(vr)) {
			s="INTEGER";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isShortTextVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isTimeVR(vr)) {
			//s="TIME";
			s="VARCHAR";	// i.e., may have fraction
		}
		else if (ValueRepresentation.isUniqueIdentifierVR(vr)) {
			s="VARCHAR";
		}
		else if (ValueRepresentation.isUnsignedLongVR(vr)) {
			s="INTEGER";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isUnknownVR(vr)) {
			s=null;
		}
		else if (ValueRepresentation.isUnsignedShortVR(vr)) {
			s="INTEGER";	// this will fail if VM > 1, so need to check to be sure only first value inserted
		}
		else if (ValueRepresentation.isUnlimitedTextVR(vr)) {
			s="VARCHAR";
		}
		else {
			s=null;		// unrecognized  ...
		}
		return s;
	}
	
	/**
	 * <p>Append a check for a match against a string value, accounting for
	 * the need to use the "IS NULL" rather than "=" expression when wanting
	 * to explciitly have NULL match NULL rather to never match.</p>
	 *
	 * @param	b	the buffer to append to
	 * @param	value	either NULL or the quoted escaped string value to append
	 */
	protected static void appendExactOrIsNullMatch(StringBuffer b,String value) {
		if (value == null || value.length() == 0 || value.equals("NULL")) {
			b.append(" IS NULL");	// this is NOT the same as = NULL
		}
		else {
			b.append(" =");		// not LIKE, too slow and not indexed and no need for wildcards
			b.append(value);	// already quoted and escaped
		}
	}

	/**
	 * <p>Get a factory to manufacture a query response generator capable of performing a query and returning the results.</p>
	 *
	 * @return					the response generator factory
	 */
	public QueryResponseGeneratorFactory getQueryResponseGeneratorFactory() {
//System.err.println("DatabaseInformationModel.getQueryResponseGeneratorFactory():");
		return null;
	}

	/**
	 * <p>Get a factory to manufacture a query response generator capable of performing a query and returning the results.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getQueryResponseGeneratorFactory()} instead.
	 * @param	debugLevel		ignored
	 * @return					the response generator factory
	 */
	public QueryResponseGeneratorFactory getQueryResponseGeneratorFactory(int debugLevel) {
		slf4jlogger.warn("getQueryResponseGeneratorFactory(): Debug level supplied as argument ignored");
		return getQueryResponseGeneratorFactory();
	}

	/**
	 * <p>Get a factory to manufacture a retrieve response generator capable of performing a retrieve and returning the results.</p>
	 *
	 * @return					the response generator factory
	 */
	public RetrieveResponseGeneratorFactory getRetrieveResponseGeneratorFactory() {
//System.err.println("DatabaseInformationModel.getRetrieveResponseGeneratorFactory():");
		return null;
	}

	/**
	 * <p>Get a factory to manufacture a retrieve response generator capable of performing a retrieve and returning the results.</p>
	 *
	 * @deprecated				SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #getRetrieveResponseGeneratorFactory()} instead.
	 * @param	debugLevel		ignored
	 * @return					the response generator factory
	 */
	public RetrieveResponseGeneratorFactory getRetrieveResponseGeneratorFactory(int debugLevel) {
		slf4jlogger.warn("getRetrieveResponseGeneratorFactory(): Debug level supplied as argument ignored");
		return getRetrieveResponseGeneratorFactory();
	}

	/**
	 * Get the information entity in this information model for the specified tag
	 *
	 * @param	tag
	 */
	public InformationEntity getInformationEntityFromTag(AttributeTag tag) {
		return dictionary.getInformationEntityFromTag(tag);
	}

	/**
	 * <p>Returns a string describing the structure (not the contents) of the database.</p>
	 *
	 * @return	a list of all the attributes for each information entities
	 * 		followed by a description of each table obtained from the
	 *		database metadata, if a connection can be established, or the string
	 * 		value of the exception if it cannot.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		// dump listsOfAttributesByInformationEntity

		buffer.append(listsOfAttributesByInformationEntity);
		buffer.append("\n");

		// dump table descriptions from database metadata

		try {
			DatabaseMetaData meta = databaseConnection.getMetaData();
			ResultSet columns = meta.getColumns(null, null, null, null);

			while (columns.next()) {
				buffer.append("catalog = "); buffer.append(columns.getString(1)); buffer.append("; ");
				buffer.append("schema  = "); buffer.append(columns.getString(2)); buffer.append("; ");
				buffer.append("table   = "); buffer.append(columns.getString(3)); buffer.append("; ");
				buffer.append("column  = "); buffer.append(columns.getString(4)); buffer.append("; ");
				buffer.append("type    = "); buffer.append(columns.getString(6)); buffer.append("; ");
				buffer.append("size    = "); buffer.append(columns.getInt(7)); buffer.append("; ");
				buffer.append("\n");
			}

			columns.close();

		}
		catch (Exception e) {
			//throw new DicomException("Cannot connect to database: "+e);
			buffer.append(e);
		}
		return buffer.toString();
	}
}
