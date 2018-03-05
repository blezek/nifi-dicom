package com.pixelmed.utils;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * <p>A class to private remote logging via the BSD syslog service (UDP on port 514).</p>
 *
 * <p>To get a syslogd to listen to port 514 packets, one needs to start it with
 * a specific option, e.g. "-r" on Linux or Solaris, or "-u" on MacOSX prior to Panther,
 * or without the "-s" after Panther. On Linux one edits the options in "/etc/sysconfig/syslog"
 * and does an "/etc/rc.d/init.d/syslog restart".</p>
 * 
 * @author	dclunie
 */

public class Syslog {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/Syslog.java,v 1.8 2016/02/04 09:00:06 dclunie Exp $";

	// Mimic constants from Unix syslog.h

	// Facility codes

	static public final int KERN	= 0<<3;		// kernel messages
	static public final int USER	= 1<<3;		// random user-level messages
	static public final int MAIL	= 2<<3;		// mail system
	static public final int DAEMON	= 3<<3;		// system daemons
	static public final int AUTH	= 4<<3;		// security/authorization messages
	static public final int SYSLOG	= 5<<3;		// messages generated internally by syslogd
	static public final int LPR	= 6<<3;		// line printer subsystem
	static public final int NEWS	= 7<<3;		// netnews subsystem
	static public final int UUCP	= 8<<3;		// uucp subsystem
							// other codes through 15 reserved for system use
	static public final int CRON	= 15<<3;	// cron/at subsystem
	static public final int LOCAL0	= 16<<3;	// reserved for local use
	static public final int LOCAL1	= 17<<3;	// reserved for local use
	static public final int LOCAL2	= 18<<3;	// reserved for local use
	static public final int LOCAL3	= 19<<3;	// reserved for local use
	static public final int LOCAL4	= 20<<3;	// reserved for local use
	static public final int LOCAL5	= 21<<3;	// reserved for local use
	static public final int LOCAL6	= 22<<3;	// reserved for local use
	static public final int LOCAL7	= 23<<3;	// reserved for local use
	
	private static Map facilityByName;
	
	private static void makeFacilityByName() {
		facilityByName = new HashMap();
		facilityByName.put("kern",new Integer(KERN));
		facilityByName.put("user",new Integer(USER));
		facilityByName.put("mail",new Integer(MAIL));
		facilityByName.put("daemon",new Integer(DAEMON));
		facilityByName.put("auth",new Integer(AUTH));
		facilityByName.put("syslog",new Integer(SYSLOG));
		facilityByName.put("lpr",new Integer(LPR));
		facilityByName.put("news",new Integer(NEWS));
		facilityByName.put("uucp",new Integer(UUCP));
		facilityByName.put("cron",new Integer(CRON));
		facilityByName.put("local0",new Integer(LOCAL0));
		facilityByName.put("local1",new Integer(LOCAL1));
		facilityByName.put("local2",new Integer(LOCAL2));
		facilityByName.put("local3",new Integer(LOCAL3));
		facilityByName.put("local4",new Integer(LOCAL4));
		facilityByName.put("local5",new Integer(LOCAL5));
		facilityByName.put("local6",new Integer(LOCAL6));
		facilityByName.put("local7",new Integer(LOCAL7));
	}

	public int getFacilityByName(String s) {
		return ((Integer)(facilityByName.get(s.toLowerCase(java.util.Locale.US)))).intValue();
	}
	
	// Priorities
	
	static public final int EMERG	= 0;  		// system is unusable
	static public final int ALERT	= 1;  		// action must be taken immediately
	static public final int CRIT	= 2;  		// critical conditions
	static public final int ERR	= 3;  		// error conditions
	static public final int WARNING	= 4;  		// warning conditions
	static public final int NOTICE	= 5;  		// normal but signification condition
	static public final int INFO	= 6;  		// informational
	static public final int DEBUG	= 7; 		// debug-level messages

	
	private static Map priorityByName;
	
	private static void makePriorityByName() {
		priorityByName = new HashMap();
		priorityByName.put("emerg",new Integer(EMERG));
		priorityByName.put("alert",new Integer(ALERT));
		priorityByName.put("crit",new Integer(CRIT));
		priorityByName.put("err",new Integer(ERR));
		priorityByName.put("warning",new Integer(WARNING));
		priorityByName.put("notice",new Integer(NOTICE));
		priorityByName.put("info",new Integer(INFO));
		priorityByName.put("debug",new Integer(DEBUG));
	}

	public int getPriorityByName(String s) {
		return ((Integer)(priorityByName.get(s.toLowerCase(java.util.Locale.US)))).intValue();
	}
	
	static private final int SYSLOG_PORT = 514;
	
	private InetAddress address;
	private DatagramSocket socket;

	public Syslog(String host) throws UnknownHostException, SocketException {
		address = InetAddress.getByName(host);
		socket = new DatagramSocket();
		socket.connect(address,SYSLOG_PORT);
		if (facilityByName == null) {
			makeFacilityByName();
		}
		if (priorityByName == null) {
			makePriorityByName();
		}
	}
	
	public final void send(int facility,int priority,String message)
			throws IOException,UnknownHostException,SocketException {
		String sData = "<"+Integer.toString(facility+priority)+">"+message;
//System.err.println(sData);
		byte[] data = sData.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(data,data.length);
		socket.send(sendPacket);
	}
	
	/**
	 * <p>Testing.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {
		try {
			Syslog syslogger = new Syslog("localhost");
			syslogger.send(syslogger.getFacilityByName("local6"),syslogger.getPriorityByName("notice"),"deidentify: dclunie - hello again more");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);		// do NOT use SLF4J here ... just confuses things
		}
	}
}

