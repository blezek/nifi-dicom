/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.slf4j;

import java.text.SimpleDateFormat;

import java.util.Date;

/**
 * <p>This class implements a thin wrapper around a subset of methods of
 * the {@link org.slf4j.Logger Logger} class from the SLF4J facade, in order to allow
 * those methods commonly used by the toolkit to be usable without invoking
 * a runtime dependency on the SLF4J jar files.</p>
 *
 * <p>For how to configure the logger properties, see the package description.</p>
 *
 * @see com.pixelmed.slf4j.LoggerFactory
 *
 * @author	dclunie
 */
public class Logger {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/slf4j/Logger.java,v 1.3 2017/01/24 10:50:47 dclunie Exp $";
	
	protected org.slf4j.Logger slf4jlogger;
	
	protected String className;
	
	protected boolean errorEnabled = true;
	protected boolean warnEnabled = true;
	protected boolean infoEnabled = true;
	protected boolean debugEnabled = false;
	protected boolean traceEnabled = false;
	
	protected boolean showDateTime = false;
	
	protected SimpleDateFormat dateFormatter;
	
	protected static boolean millisecondsSinceEpochAtStartIsSet;
	protected static long millisecondsSinceEpochAtStart;
	
	/**
	 * <p>Is the logger instance enabled for the ERROR level?</p>
	 *
	 * return	True if this Logger is enabled for the ERROR level, false otherwise.
	 */
	public boolean isErrorEnabled() { return errorEnabled; }
	
	/**
	 * <p>Is the logger instance enabled for the WARN level?</p>
	 *
	 * return	True if this Logger is enabled for the WARN level, false otherwise.
	 */
	public boolean isWarnEnabled() { return warnEnabled; }
	
	/**
	 * <p>Is the logger instance enabled for the INFO level?</p>
	 *
	 * return	True if this Logger is enabled for the INFO level, false otherwise.
	 */
	public boolean isInfoEnabled() { return infoEnabled; }
	
	/**
	 * <p>Is the logger instance enabled for the DEBUG level?</p>
	 *
	 * return	True if this Logger is enabled for the DEBUG level, false otherwise.
	 */
	public boolean isDebugEnabled() { return debugEnabled; }
	
	/**
	 * <p>Is the logger instance enabled for the TRACE level?</p>
	 *
	 * return	True if this Logger is enabled for the TRACE level, false otherwise.
	 */
	public boolean isTraceEnabled() { return traceEnabled; }

	protected void setLoggingDetailLevel(String level) {
		switch (level) {
			case "trace":
				traceEnabled = true;
				debugEnabled = true;
				infoEnabled = true;
				warnEnabled = true;
				errorEnabled = true;
				break;
			case "debug":
				traceEnabled = false;
				debugEnabled = true;
				infoEnabled = true;
				warnEnabled = true;
				errorEnabled = true;
				break;
			case "info":
				traceEnabled = false;
				debugEnabled = false;
				infoEnabled = true;
				warnEnabled = true;
				errorEnabled = true;
				break;
			case "warn":
				traceEnabled = false;
				debugEnabled = false;
				infoEnabled = false;
				warnEnabled = true;
				errorEnabled = true;
				break;
			case "error":
				traceEnabled = false;
				debugEnabled = false;
				infoEnabled = false;
				warnEnabled = false;
				errorEnabled = true;
				break;
		}
	}
	
	/**
	 * <p>Construct a logger that uses an slf4j Logger.</p>
	 *
	 * @param	slf4jlogger	an slf4jlogger
	 */
	protected Logger(org.slf4j.Logger slf4jlogger) {
		this.slf4jlogger = slf4jlogger;
//System.err.println("Logger(org.slf4j.Logger "+slf4jlogger.getName()+"): isDebugEnabled = "+slf4jlogger.isDebugEnabled());
//System.err.println("Logger(org.slf4j.Logger "+slf4jlogger.getName()+"): isTraceEnabled = "+slf4jlogger.isTraceEnabled());
	}

	/**
	 * <p>Construct a default logger named corresponding to the class name passed as parameter.</p>
	 *
	 * @param	className	the name of the logger
	 */
	protected Logger(String className) {
		// see https://github.com/qos-ch/slf4j/blob/master/slf4j-simple/src/test/resources/simplelogger.properties
		this.className=className;
		String classSpecificLoggingDetailLevelProperty = System.getProperty("org.slf4j.simpleLogger.log."+className);
		if (classSpecificLoggingDetailLevelProperty != null) {
			setLoggingDetailLevel(classSpecificLoggingDetailLevelProperty);
		}
		else {
			String defaultLoggingDetailLevelProperty = System.getProperty("org.slf4j.simpleLogger.defaultLogLevel");
			if (defaultLoggingDetailLevelProperty != null) {
				setLoggingDetailLevel(defaultLoggingDetailLevelProperty);
			}
			// else leave as set in class declarations
		}
		
		{
			String showDateTimeProperty = System.getProperty("org.slf4j.simpleLogger.showDateTime");
			if ("true".equals(showDateTimeProperty)) {
				showDateTime = true;
				String dateTimeFormatProperty = System.getProperty("org.slf4j.simpleLogger.dateTimeFormat");
				if (dateTimeFormatProperty == null && !millisecondsSinceEpochAtStartIsSet) {	// make sure we only set this once
					millisecondsSinceEpochAtStart = new Date().getTime();
				}
				else {
					try {
						dateFormatter = new SimpleDateFormat(dateTimeFormatProperty);
					}
					catch (Throwable t) {
						// do not allow logging to derail application
						System.err.println("Logger(): exception while creating SimpleDateFormat from \""+dateTimeFormatProperty+"\"");
						t.printStackTrace(System.err);
					}
				}
			}
		}
	}
	
	/**
	 * <p>Log a message at the ERROR level.</p>
	 *
	 * @param	msg	the message string to be logged
	 */
	public void error(String msg) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isErrorEnabled()) {
				slf4jlogger.error(msg);
			}
		}
		else if (errorEnabled) {
			System.err.println(getPreamable("ERROR")+msg);
		}
	}
	
	/**
	 * <p>Log a message at the ERROR level.</p>
	 *
	 * @param	format	the format string
	 * @param	arguments	a list of 1 or more arguments
	 */
	public void error(String format,Object... arguments) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isErrorEnabled()) {
				slf4jlogger.error(format,arguments);
			}
		}
		else if (errorEnabled) {
			System.err.println(getPreamable("ERROR")+getFormattedStringFromArguments(format,arguments));
		}
	}
	
	/**
	 * <p>Log an exception (throwable) at the ERROR level with an accompanying message.</p>
	 *
	 * @param	msg	the message accompanying the exception
	 * @param	t	the exception (throwable) to log
	 */
	public void error(String msg,Throwable t) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isErrorEnabled()) {
				slf4jlogger.error(msg,t);
			}
		}
		else if (errorEnabled) {
			System.err.println(getPreamable("ERROR")+msg+t);
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * <p>Log a message at the WARN level.</p>
	 *
	 * @param	msg	the message string to be logged
	 */
	public void warn(String msg) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isWarnEnabled()) {
				slf4jlogger.warn(msg);
			}
		}
		else if (warnEnabled) {
			System.err.println(getPreamable("WARN")+msg);
		}
	}
	
	/**
	 * <p>Log a message at the WARN level.</p>
	 *
	 * @param	format	the format string
	 * @param	arguments	a list of 1 or more arguments
	 */
	public void warn(String format,Object... arguments) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isWarnEnabled()) {
				slf4jlogger.warn(format,arguments);
			}
		}
		else if (warnEnabled) {
			System.err.println(getPreamable("WARN")+getFormattedStringFromArguments(format,arguments));
		}
	}
	
	/**
	 * <p>Log an exception (throwable) at the WARN level with an accompanying message.</p>
	 *
	 * @param	msg	the message accompanying the exception
	 * @param	t	the exception (throwable) to log
	 */
	public void warn(String msg,Throwable t) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isWarnEnabled()) {
				slf4jlogger.warn(msg,t);
			}
		}
		else if (warnEnabled) {
			System.err.println(getPreamable("WARN")+msg+t);
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * <p>Log a message at the INFO level.</p>
	 *
	 * @param	msg	the message string to be logged
	 */
	public void info(String msg) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isInfoEnabled()) {
				slf4jlogger.info(msg);
			}
		}
		else if (infoEnabled) {
			System.err.println(getPreamable("INFO")+msg);
		}
	}
	
	/**
	 * <p>Log a message at the INFO level.</p>
	 *
	 * @param	format	the format string
	 * @param	arguments	a list of 1 or more arguments
	 */
	public void info(String format,Object... arguments) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isInfoEnabled()) {
				slf4jlogger.info(format,arguments);
			}
		}
		else if (infoEnabled) {
			System.err.println(getPreamable("INFO")+getFormattedStringFromArguments(format,arguments));
		}
	}
	
	/**
	 * <p>Log an exception (throwable) at the INFO level with an accompanying message.</p>
	 *
	 * @param	msg	the message accompanying the exception
	 * @param	t	the exception (throwable) to log
	 */
	public void info(String msg,Throwable t) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isInfoEnabled()) {
				slf4jlogger.info(msg,t);
			}
		}
		else if (infoEnabled) {
			System.err.println(getPreamable("INFO")+msg+t);
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * <p>Log a message at the DEBUG level.</p>
	 *
	 * @param	msg	the message string to be logged
	 */
	public void debug(String msg) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isDebugEnabled()) {
				slf4jlogger.debug(msg);
			}
		}
		else if (debugEnabled) {
			System.err.println(getPreamable("DEBUG")+msg);
		}
	}
	
	/**
	 * <p>Log a message at the DEBUG level.</p>
	 *
	 * @param	format	the format string
	 * @param	arguments	a list of 1 or more arguments
	 */
	public void debug(String format,Object... arguments) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isDebugEnabled()) {
				slf4jlogger.debug(format,arguments);
			}
		}
		else if (debugEnabled) {
			System.err.println(getPreamable("DEBUG")+getFormattedStringFromArguments(format,arguments));
		}
	}
	
	/**
	 * <p>Log an exception (throwable) at the DEBUG level with an accompanying message.</p>
	 *
	 * @param	msg	the message accompanying the exception
	 * @param	t	the exception (throwable) to log
	 */
	public void debug(String msg,Throwable t) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isDebugEnabled()) {
				slf4jlogger.debug(msg,t);
			}
		}
		else if (debugEnabled) {
			System.err.println(getPreamable("DEBUG")+msg+t);
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * <p>Log a message at the TRACE level.</p>
	 *
	 * @param	msg	the message string to be logged
	 */
	public void trace(String msg) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isTraceEnabled()) {
				slf4jlogger.trace(msg);
			}
		}
		else if (traceEnabled) {
			System.err.println(getPreamable("TRACE")+msg);
		}
	}
	
	/**
	 * <p>Log a message at the TRACE level.</p>
	 *
	 * @param	format	the format string
	 * @param	arguments	a list of 1 or more arguments
	 */
	public void trace(String format,Object... arguments) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isTraceEnabled()) {
				slf4jlogger.trace(format,arguments);
			}
		}
		else if (traceEnabled) {
			System.err.println(getPreamable("TRACE")+getFormattedStringFromArguments(format,arguments));
		}
	}
	
	/**
	 * <p>Log an exception (throwable) at the TRACE level with an accompanying message.</p>
	 *
	 * @param	msg	the message accompanying the exception
	 * @param	t	the exception (throwable) to log
	 */
	public void trace(String msg,Throwable t) {
		if (slf4jlogger != null) {
			if (slf4jlogger.isTraceEnabled()) {
				slf4jlogger.trace(msg,t);
			}
		}
		else if (traceEnabled) {
			System.err.println(getPreamable("TRACE")+msg+t);
			t.printStackTrace(System.err);
		}
	}
	
	protected String getPreamable(String level) {
		String dateTime = "";
		if (showDateTime) {
			if (dateFormatter != null) {
				try {
					dateTime = dateFormatter.format(new Date()) + " ";
				}
				catch (Throwable t) {
					System.err.println("Logger.getPreamble(): exception while formatting current date and time");
				}
			}
			else {
				dateTime = Long.toString(new Date().getTime() - millisecondsSinceEpochAtStart) + " ";
			}
		}
		String threadNameString = "";
		{
			try {
				threadNameString = "[" + Thread.currentThread().getName() + "] ";
			}
			catch (Throwable t) {
				System.err.println("Logger.getPreamble(): exception while getting current thread name");
			}
		}
		
		return	  dateTime
				+ threadNameString
				+ level
				+ " "
				+ className
				+ " ";
	}

	protected static String getFormattedStringFromArguments(String format,Object... arguments) {
//System.err.println("Logger.getFormattedStringFromArguments(\""+format+"\",...["+arguments.length+"])");
		String result=format;
		try {
			String[] formatSplit = format.split("[{][}]");
//System.err.println("Logger.getFormattedStringFromArguments(): formatSplit.length = "+formatSplit.length);
			if (formatSplit.length > 0) {
				if (formatSplit.length >= arguments.length) {
					StringBuffer buf = new StringBuffer();
					buf.append(formatSplit[0]);
					for (int i=0; i<arguments.length; ++i) {
						buf.append(arguments[i]);
						if (i+1 < formatSplit.length) {
							buf.append(formatSplit[i+1]);
						}
					}
					result = buf.toString();
				}
				else {
					System.err.println("Logger.getFormattedStringFromArguments(): too few arguments in format string \""+format+"\" when given "+arguments.length+" arguments");
				}
			}
		}
		catch (Throwable t) {
			// do not allow logging to derail application
			System.err.println("Logger.getFormattedStringFromArguments(): exception while replacing arguments in format string \""+format+"\"");
			t.printStackTrace(System.err);
		}
		return result;
	}
}
