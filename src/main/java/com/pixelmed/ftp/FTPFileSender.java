/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

import com.pixelmed.display.SafeProgressBarUpdaterThread;
import com.pixelmed.display.event.StatusChangeEvent;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.utils.MessageLogger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import org.apache.commons.net.PrintCommandListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.UUID;

import java.security.NoSuchAlgorithmException;

import javax.swing.JProgressBar;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to send files via FTP or secure FTP over TLS.</p>
 *
 * @author	dclunie
 */
public class FTPFileSender {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPFileSender.java,v 1.12 2017/01/24 10:50:43 dclunie Exp $";
	
	private static final Logger slf4jlogger = LoggerFactory.getLogger(FTPFileSender.class);		// do not confuse with MessageLogger

	protected static int socketConnectTimeoutInMilliSeconds = 30000;
	
	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @deprecated								SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #FTPFileSender(FTPRemoteHost,String[],boolean,MessageLogger,JProgressBar)} instead.
	 * @param	remoteHost						the characteristics of the remote host
	 * @param	files							a String array of local filenames to send
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 * @param	debugLevel						ignored
	 * @param	logger							where to send routine logging messages (may be null)
	 * @param	progressBar						where to send progress updates (may be null)
	 */
	public FTPFileSender(FTPRemoteHost remoteHost,String[] files,boolean generateRandomRemoteFileNames,int debugLevel,MessageLogger logger,JProgressBar progressBar) throws NoSuchAlgorithmException, IOException, Exception {
		this(remoteHost,files,generateRandomRemoteFileNames,logger,progressBar);
		slf4jlogger.warn("gDebug level supplied as constructor argument ignored");
	}
	
	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @param	remoteHost						the characteristics of the remote host
	 * @param	files							a String array of local filenames to send
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 * @param	logger							where to send routine logging messages (may be null)
	 * @param	progressBar						where to send progress updates (may be null)
	 */
	public FTPFileSender(FTPRemoteHost remoteHost,String[] files,boolean generateRandomRemoteFileNames,MessageLogger logger,JProgressBar progressBar) throws NoSuchAlgorithmException, IOException, Exception {
		this(remoteHost.getHost(),remoteHost.getUser(),remoteHost.getPassword(),remoteHost.getDirectory(),files,remoteHost.getSecurity().equals(FTPSecurityType.TLS),generateRandomRemoteFileNames,logger,progressBar);
	}
	
	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @deprecated								SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #FTPFileSender(String,String,String,String,String[],boolean secure,boolean)} instead.
	 * @param	server							the hostname or IP address of the server
	 * @param	username						the username for login
	 * @param	password						the password for login
	 * @param	remoteDirectory					the remote directory to upload the files to (may be null if the root directory is to be used)
	 * @param	files							a String array of local filenames to send
	 * @param	secure							whether or not to use secure ftp over tls, or ordinary ftp
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 * @param	debugLevel						ignored
	 */
	public FTPFileSender(String server,String username,String password,String remoteDirectory,String[] files,boolean secure,boolean generateRandomRemoteFileNames,int debugLevel) throws NoSuchAlgorithmException, IOException, FTPException {
		this(server,username,password,remoteDirectory,files,secure,generateRandomRemoteFileNames);
		slf4jlogger.warn("gDebug level supplied as constructor argument ignored");
	}

	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @param	server							the hostname or IP address of the server
	 * @param	username						the username for login
	 * @param	password						the password for login
	 * @param	remoteDirectory					the remote directory to upload the files to (may be null if the root directory is to be used)
	 * @param	files							a String array of local filenames to send
	 * @param	secure							whether or not to use secure ftp over tls, or ordinary ftp
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 */
	public FTPFileSender(String server,String username,String password,String remoteDirectory,String[] files,boolean secure,boolean generateRandomRemoteFileNames) throws NoSuchAlgorithmException, IOException, FTPException {
		this(server,username,password,remoteDirectory,files,secure,generateRandomRemoteFileNames,null,null);
	}
	
	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @deprecated								SLF4J is now used instead of debugLevel parameters to control debugging - use {@link #FTPFileSender(String,String,String,String,String[],boolean secure,boolean,MessageLogger,JProgressBar)} instead.
	 * @param	server							the hostname or IP address of the server
	 * @param	username						the username for login
	 * @param	password						the password for login
	 * @param	remoteDirectory					the remote directory to upload the files to (may be null if the root directory is to be used)
	 * @param	files							a String array of local filenames to send
	 * @param	secure							whether or not to use secure ftp over tls, or ordinary ftp
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 * @param	debugLevel						ignored
	 * @param	logger							where to send routine logging messages (may be null)
	 * @param	progressBar						where to send progress updates (may be null)
	 */
	public FTPFileSender(String server,String username,String password,String remoteDirectory,String[] files,boolean secure,boolean generateRandomRemoteFileNames,int debugLevel,MessageLogger logger,JProgressBar progressBar) throws NoSuchAlgorithmException, IOException, FTPException {
		this(server,username,password,remoteDirectory,files,secure,generateRandomRemoteFileNames,logger,progressBar);
		slf4jlogger.warn("Debug level supplied as constructor argument ignored");
	}

	/**
	 * <p>Construct an ftp connection to send a list of files to a remote server.</p>
	 *
	 * <p>Sends a list of files to a single remote directory. Note that if the supplied local file names
	 * have the same base name (same name in different local directories) then they wil overwrite each
	 * other in the single remote directory; hence the option to generate random remote names.</p>
	 *
	 * @param	server							the hostname or IP address of the server
	 * @param	username						the username for login
	 * @param	password						the password for login
	 * @param	remoteDirectory					the remote directory to upload the files to (may be null if the root directory is to be used)
	 * @param	files							a String array of local filenames to send
	 * @param	secure							whether or not to use secure ftp over tls, or ordinary ftp
	 * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
	 * @param	logger							where to send routine logging messages (may be null)
	 * @param	progressBar						where to send progress updates (may be null)
	 */
	public FTPFileSender(String server,String username,String password,String remoteDirectory,String[] files,boolean secure,boolean generateRandomRemoteFileNames,MessageLogger logger,JProgressBar progressBar) throws NoSuchAlgorithmException, IOException, FTPException {
		SafeProgressBarUpdaterThread progressBarUpdater = null;
		if (progressBar != null) {
			progressBarUpdater =  new SafeProgressBarUpdaterThread(progressBar);
		}
		FTPClient ftp = secure ? new FTPSClient("TLS",false/*isImplicit*/) : new FTPClient();
		slf4jlogger.debug("FTPClient original connect timeout = {} ms",ftp.getConnectTimeout());
		ftp.setConnectTimeout(socketConnectTimeoutInMilliSeconds);
		slf4jlogger.debug("FTPClient replaced connect timeout = {} ms",ftp.getConnectTimeout());
		if (slf4jlogger.isDebugEnabled()) ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err)));	// This is not right ... need to find a way to create PrintWriter that sends to SLF4H :(
		try {
			int reply;
			if (secure) {
				try {
					slf4jlogger.debug("Trying to connect in explicit mode to {}",server);
					ftp.connect(server);
				}
				catch (Exception e) {
					slf4jlogger.debug("Failed to connect in explicit mode to {}",server,e);
					slf4jlogger.debug("Trying again in implicit mode on port 990");
					// failed so try implicit mode on port 990
					ftp = new FTPSClient("TLS",true/*isImplicit*/);
					slf4jlogger.debug("FTPClient original connect timeout = {} ms",ftp.getConnectTimeout());
					ftp.setConnectTimeout(socketConnectTimeoutInMilliSeconds);
					slf4jlogger.debug("FTPClient replaced connect timeout = {} ms",ftp.getConnectTimeout());
					ftp.setDefaultPort(990);
					if (slf4jlogger.isDebugEnabled()) ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err)));	// This is not right ... need to find a way to create PrintWriter that sends to SLF4H :(
					slf4jlogger.debug("About to connect");
					ftp.connect(server);
					slf4jlogger.debug("Back from connect");
				}
			}
			else {
				// Let any failure fall through with no retry ...
				ftp.connect(server);
			}
			slf4jlogger.debug("Connected to {}",server);
			slf4jlogger.debug(ftp.getReplyString());
			reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				throw new FTPException("FTP server "+server+" refused connection");
			}
			if (secure) {
				((FTPSClient)ftp).execPBSZ(0);			// required, but only value permitted is 0
				((FTPSClient)ftp).execPROT("P");		// otherwise transfers will be unencrypted
			}
			if (!ftp.login(username, password)) {
				ftp.disconnect();
				throw new FTPException("FTP server "+server+" login failed");
			}
			// transfer files
			
			ftp.enterLocalPassiveMode();
			
			if (remoteDirectory != null && remoteDirectory.length() > 0) {
				if (!ftp.changeWorkingDirectory(remoteDirectory)) {
					ftp.disconnect();
					throw new FTPException("FTP server "+server+" cwd to "+remoteDirectory+" failed");
				}
				slf4jlogger.debug("Working directory is now {}",ftp.printWorkingDirectory());
			}

			if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
				ftp.disconnect();
				throw new FTPException("FTP server "+server+" set file type to Binary failed");
			}

			ApplicationEventDispatcher applicationEventDispatcher = ApplicationEventDispatcher.getApplicationEventDispatcher();
			int maximum = files.length;
			SafeProgressBarUpdaterThread.startProgressBar(progressBarUpdater,maximum);
			int done=0;
			for (String localFilename: files) {
				SafeProgressBarUpdaterThread.updateProgressBar(progressBarUpdater,done);
				File localFile = new File(localFilename);
				InputStream i = new FileInputStream(localFile);
				String remoteFilename = generateRandomRemoteFileNames ? UUID.randomUUID().toString() : localFile.getName();
				slf4jlogger.debug("Attempting to store local {} to remote {}",localFilename,remoteFilename);
				if (!ftp.storeFile(remoteFilename,i)) {
					ftp.disconnect();
					throw new FTPException("FTP server "+server+" file store of local "+localFilename+" to remote "+remoteFilename+" failed");
				}
				i.close();
				slf4jlogger.debug("Successfully stored local {} to remote {}",localFilename,remoteFilename);
				if (logger != null) {
					logger.sendLn("Successfully stored local "+localFilename+" to remote "+remoteFilename);
				}
				SafeProgressBarUpdaterThread.endProgressBar(progressBarUpdater);
				if (applicationEventDispatcher != null) {
					applicationEventDispatcher.processEvent(new StatusChangeEvent("Sent "+localFilename+" to Registry"));
				}
				++done;
			}

			ftp.logout();
		}
		finally {
			if(ftp.isConnected()) {
				slf4jlogger.debug("FTPFileSender(): finally so disconnect");
				ftp.disconnect();
			}
		}
	}

	public static void main(String arg[]) {
		try {
			String server          = arg[0];
			String username        = arg[1];
			String password        = arg[2];
			String remoteDirectory = arg[3];
			if (remoteDirectory.equals("-") || remoteDirectory.equals(".")) {
				remoteDirectory = null;
			}
			boolean secure = arg[4].toUpperCase(java.util.Locale.US).trim().equals("SECURE");
			boolean generateRandomRemoteFileNames = arg[5].toUpperCase(java.util.Locale.US).trim().equals("RANDOM");
			
			int numberOfFiles = arg.length - 6;
			String[] files = new String[numberOfFiles];
			System.arraycopy(arg,6,files,0,numberOfFiles);
			new FTPFileSender(server,username,password,remoteDirectory,files,secure,generateRandomRemoteFileNames);
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
			System.exit(1);
		}
	}
}

