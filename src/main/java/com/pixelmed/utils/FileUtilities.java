/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>Various static methods helpful for handling files.</p>
 *
 * @author	dclunie
 */
public class FileUtilities {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/FileUtilities.java,v 1.25 2017/01/24 10:50:51 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(FileUtilities.class);

	private FileUtilities() {}

	/**
	 * <p>Rename a file, if possible, else make a copy of it.</p>
	 *
	 * @param	srcFile			the source
	 * @param	dstFile			the destination
	 * @throws	IOException	thrown if the copying fails for any reason
	 */
	static public final void renameElseCopyTo(File srcFile,File dstFile) throws IOException {
//System.err.println("FileUtilities.renameElseCopyTo(): renaming "+srcFile+" to "+dstFile);
		if (!srcFile.renameTo(dstFile)) {
//System.err.println("FileUtilities.renameElseCopyTo(): renaming failed, so copy instead");
			CopyStream.copy(srcFile,dstFile);
//System.err.println("FileUtilities.renameElseCopyTo(): copying succeeded");
		}
	}

	/**
	 * <p>Recursively traverse the specified directory and its sub-directory and
	 * produce a list of all the files contained therein, in no particular order.</p>
	 *
	 * <p>If the path is a file, just return that.</p>
	 *
	 * <p>Any security (permission) exceptions are caught and logged to stderr
	 * and not propagated.</p>
	 *
	 * @param	initialPath	The abstract pathname of the directory to begin searching
	 * @return			An ArrayList of abstract pathnames denoting the files found.
	 *				The ArrayList will be empty if the path is empty or does not exist
	 *				or if an error occurs.
	 */
	static public final ArrayList<File> listFilesRecursively(File initialPath) {
//System.err.println("FileUtilities.listFilesRecursively(): "+initialPath);
		ArrayList filesFound = new ArrayList();
		if (initialPath != null && initialPath.exists()) {
			if (initialPath.isFile()) {
				filesFound.add(initialPath);
			}
			else if (initialPath.isDirectory()) {
				try {
					File[] filesAndDirectories = initialPath.listFiles((FilenameFilter)null);	// null FilenameFilter means all names
					if (filesAndDirectories != null && filesAndDirectories.length > 0) {
						for (int i=0; i<filesAndDirectories.length; ++i) {
							if (filesAndDirectories[i].isDirectory()) {
								ArrayList moreFiles = listFilesRecursively(filesAndDirectories[i]);
								if (moreFiles != null && !moreFiles.isEmpty()) {
									filesFound.addAll(moreFiles);
								}
							}
							else if (filesAndDirectories[i].isFile()) {			// what else could it be ... just being paranoid
//System.err.println("FileUtilities.listFilesRecursively(): found "+filesAndDirectories[i]);
								filesFound.add(filesAndDirectories[i]);
							}
						}
					}
				}
				catch (SecurityException e) {
					slf4jlogger.error("", e);
				}
			}
			// else what else could it be
		}
		return filesFound;
	}
	
	static public final ArrayList<String> getCanonicalFileNames(ArrayList<File> files) throws IOException {
		ArrayList<String> filenames = new ArrayList<String>();
		for (File f : files) {
			filenames.add(f.getCanonicalPath());
		}
		return filenames;
	}

	/**
	 * <p>Read an entire file into a string.</p>
	 *
	 * @param	reader		The file reader
	 * @return			The contents of the file as a <code>String</code>.
	 * @throws	IOException	If an IO error occurs.
	 */
	static public final String readFile(Reader reader) throws IOException {
		StringBuffer strbuf = new StringBuffer();
		char[] charbuf = new char[1024];
		int count;
		while ((count=reader.read(charbuf)) > 0) {
			strbuf.append(charbuf,0,count);
		}
		return strbuf.toString();
	}

	/**
	 * <p>Read an entire file into a string.</p>
	 *
	 * @param	stream		The input stream (e.g., from <code>class.getResourceAsStream()</code>)
	 * @return			The contents of the file as a <code>String</code>.
	 * @throws	IOException	If an IO error occurs.
	 */
	static public final String readFile(InputStream stream) throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(stream));
		return readFile(reader);
	}


	/**
	 * <p>Read an entire file into a string.</p>
	 *
	 * @param	file		The file
	 * @return			The contents of the file as a <code>String</code>.
	 * @throws	IOException	If an IO error occurs.
	 */
	static public final String readFile(File file) throws IOException {
		Reader reader = new FileReader(file);
		return readFile(reader);
	}

	/**
	 * <p>Read an entire file into a string.</p>
	 *
	 * @param	filename	The file
	 * @return			The contents of the file as a <code>String</code>.
	 * @throws	IOException	If an IO error occurs.
	 */
	static public final String readFile(String filename) throws IOException {
		return readFile(new File(filename));
	}

	/**
	 * <p>Determine if a file corresponding to the specified name exists,
	 * checking for case insensitive variants if necessary.</p>
	 *
	 * @param	fileName	The name of the file to find
	 * @return			A file if found.
	 * @throws	FileNotFoundException	If the file cannot be found.
	 */
	static public final File getFileFromNameInsensitiveToCaseIfNecessary(String fileName) throws FileNotFoundException {
		File file = new File(fileName);
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
		if (!file.exists()) {
			file = new File(fileName.toLowerCase(java.util.Locale.US));
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
			if (!file.exists()) {
				file = new File(fileName.toUpperCase(java.util.Locale.US));
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
				if (!file.exists()) {
					// Try doing it one component at a time checking case independently for each
					file = new File(fileName).getAbsoluteFile();
					ArrayList list = new ArrayList();
					String name = null;
					while (file != null && (name=file.getName()) != null && name.length() > 0) {
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Adding name component "+name);
						list.add(name);
						file=file.getParentFile();
					}
					File roots[] = File.listRoots();
					if (roots != null) {
						file = null;
						for (int r=0; file == null && r<roots.length; ++r) {
							File root = roots[r];
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying root "+root);
							for (int i=list.size()-1; i>=0; --i) {
								name = (String)(list.get(i));
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying next name component "+name);
								file = new File(root,name);
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
								if (!file.exists()) {
									file = new File(root,name.toLowerCase(java.util.Locale.US));
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
									if (!file.exists()) {
										file = new File(root,name.toUpperCase(java.util.Locale.US));
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Trying "+file);
										if (!file.exists()) {
											file = null;		// will try other roots if any
										}
									}
								}
								root = file;
							}
						}
						if (file == null) {
							throw new FileNotFoundException(fileName + "(No such file or lower or upper case variants)");
						}
					}
				}
			}
		}
//System.err.println("FileUtilities.getFileFromNameInsensitiveToCaseIfNecessary(): Found "+file);
		return file;
	}
	
	/**
	 * <p>Create a temporary filename.</p>
	 *
	 * @return			a string that does not include delimiter characters and is unique within this JVM.
	 */
	static public final String makeTemporaryFileName()  {
		String fileName = new java.rmi.server.UID().toString().replaceAll("[^A-Za-z0-9 ]","_").toUpperCase(java.util.Locale.US) + ".tmp";
		return fileName;
	}

	/**
	 * <p>Given a file name, such as the properties file name, make a path to it in the user's home directory.</p>
	 *
	 * @param	fileName	 the file name to make a path to
	 */
	static public final String makePathToFileInUsersHomeDirectory(String fileName) {
		return System.getProperty("user.home")+System.getProperty("file.separator")+fileName;
	}

	/**
	 * <p>Return a message digest of a file.</p>
	 *
	 * @param	fileName	the file name
	 * @param	algorithm	the digest algorithm, such as "MD5" or "SHA"
	 * @return				string representation of the digest
	 */
	static public final String digest(String fileName,String algorithm) throws IOException, NoSuchAlgorithmException {
		return digest(new FileInputStream(fileName),algorithm);
	}
	
	/**
	 * <p>Return a message digest of an InputStream.</p>
	 *
	 * @param	in			the InputStream
	 * @param	algorithm	the digest algorithm, such as "MD5" or "SHA"
	 * @return				string representation of the digest
	 */
	static public final String digest(InputStream in,String algorithm) throws IOException, NoSuchAlgorithmException {
		// See "http://stackoverflow.com/questions/304268/using-java-to-get-a-files-md5-checksum"
		
		int readBufferSize = 32768;
		byte[] readBuffer = new byte[readBufferSize];
		MessageDigest md = MessageDigest.getInstance(algorithm);
		try {
			in = new DigestInputStream(in, md);
			while (in.read(readBuffer,0,readBufferSize) > 0);
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			in.close();
		}
		byte[] bValue = md.digest();	// complete the hash computation
		String sValue = HexDump.byteArrayToHexString(bValue);
		return sValue;
	}


	/**
	 * <p>Return an MD5 message digest of a file.</p>
	 *
	 * @param	fileName	the file name
	 * @return				string representation of the digest
	 */
	static public final String md5(String fileName) throws IOException, NoSuchAlgorithmException {
		return digest(fileName,"MD5");
	}

	/**
	 * <p>Return an MD5 message digest of an InputStream.</p>
	 *
	 * @param	in			the InputStream
	 * @return				string representation of the digest
	 */
	static public final String md5(InputStream in) throws IOException, NoSuchAlgorithmException {
		return digest(in,"MD5");
	}

	/**
	 * <p>Get the individual components of the canonical form of the path as a list.</p>
	 *
	 * @param	path
	 * @return			each component of the path, starting with the root
	 */
	public static List<String> getFilePathComponents(File path) throws IOException {
		return getFilePathComponents(path.getCanonicalFile(),new ArrayList<String>());
	}

	private static List<String> getFilePathComponents(File path,List<String> list) {
		String name = path.getName();
		if (name != null && name.length() > 0) {
			list.add(0,name);	// adds to the head of the list
		}
		File parent = path.getParentFile();
		if (parent != null) {
			getFilePathComponents(parent,list);
		}
		return list;
	}
	
	/**
	 * <p>Create a new path that re-creates the relative path of the source file in the destination folder.</p>
	 *
	 * @param	srcFolderName
	 * @param	dstFolderName
	 * @param	srcFileName
	 * @return					a File in the destination folder
	 */
	public static File makeSameRelativePathNameInDifferentFolder(String srcFolderName,String dstFolderName,String srcFileName) throws IOException {
		return makeSameRelativePathNameInDifferentFolder(new File(srcFolderName),new File(dstFolderName),new File(srcFileName));
	}
	
	/**
	 * <p>Create a new path that re-creates the relative path of the source file in the destination folder.</p>
	 *
	 * @param	srcFolder
	 * @param	dstFolder
	 * @param	srcFile
	 * @return					a File in the destination folder
	 */
	public static File makeSameRelativePathNameInDifferentFolder(File srcFolder,File dstFolder,File srcFile) throws IOException {
		List<String> srcFolderComponents = getFilePathComponents(srcFolder);
		List<String> srcFileComponents = getFilePathComponents(srcFile);
		int i = 0;
		while (i < srcFolderComponents.size() && i < srcFileComponents.size() && srcFolderComponents.get(i).equals(srcFileComponents.get(i))) ++i;
		// i is now first position where they differ
		File dstFile = dstFolder;
		while (i < srcFileComponents.size()) {
			dstFile = new File(dstFile,srcFileComponents.get(i++));
		}
		return dstFile;
	}
}


