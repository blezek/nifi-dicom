/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.slf4j;

/**
 * <p>This class implements a thin wrapper around a subset of methods of
 * the {@link org.slf4j.LoggerFactory LoggerFactory} class from the SLF4J facade, in order to allow
 * those methods commonly used by the toolkit to be usable without invoking
 * a runtime dependency on the SLF4J jar files.</p>
 *
 * <p>The {@link org.slf4j.LoggerFactory LoggerFactory} produces
 * a {@link org.slf4j.LoggerFactory LoggerFactory} instance if the slf4j-api
 * and an slf4j implementation are present at run time.</p>
 *
 * <p>Otherwise it mimics the behavior of the slf4j-simple implementation
 * and writes messages to {@link java.lang.System#err System.err}.</p>
 *
 * <p>For how to configure the logger properties, see the package description.</p>
 *
 * @see com.pixelmed.slf4j.Logger
 *
 * @author	dclunie
 */
public class LoggerFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/slf4j/LoggerFactory.java,v 1.3 2017/01/24 10:50:47 dclunie Exp $";
	
	protected static Class<?> slf4jClass = null;	// the <?> shuts up "warning: [unchecked] unchecked call to getConstructor(Class<?>...) as a member of the raw type Class"
	static {
		try {
			slf4jClass = Thread.currentThread().getContextClassLoader().loadClass("org.slf4j.LoggerFactory");
		}
		catch (ClassNotFoundException e) {
			// no problem ... it wasn't found so leave slf4jClass as null
		}
	}
	
	/**
	 * <p>Return a logger named corresponding to the class passed as parameter.</p>
	 *
	 * <p>Be warned that if slf4j.detectLoggerNameMismatch system property is set to true
	 * at run time (the default is false), a logger name mismatch warning will always be
	 * printed when an slf4j implementation is present.</p>
	 *
	 * @param	clazz	the returned logger will be named after clazz
	 */
	public static com.pixelmed.slf4j.Logger getLogger(Class<?> clazz) {
		com.pixelmed.slf4j.Logger logger = null;
		if (slf4jClass != null) {
			Class<?> [] argTypes  = {Class.class};		// the <?> makes no difference (no warning without it)
			Object[] argValues = {clazz};
			//Class [] argTypes  = {String.class};
			//Object[] argValues = {clazz == null ? "" : clazz.getName()};
			try {
				org.slf4j.Logger slf4jlogger = (org.slf4j.Logger)(slf4jClass.getDeclaredMethod("getLogger",argTypes).invoke(null/*since static method*/,argValues));
				logger = new com.pixelmed.slf4j.Logger(slf4jlogger);
			}
			catch (NoSuchMethodException e) {
				// this should not happen, but c'est la vie ... fall through and create our own
				System.err.println("Error: Could not construct slf4jlogger for class "+clazz);
				e.printStackTrace(System.err);
			}
			catch (IllegalAccessException e) {
				// this should not happen, but c'est la vie ... fall through and create our own
				System.err.println("Error: Could not construct slf4jlogger for class "+clazz);
				e.printStackTrace(System.err);
			}
			catch (java.lang.reflect.InvocationTargetException e) {
				// this should not happen, but c'est la vie ... fall through and create our own
				System.err.println("Error: Could not construct slf4jlogger for class "+clazz);
				e.printStackTrace(System.err);
			}
		}
		if (logger == null) {
			logger = new com.pixelmed.slf4j.Logger(clazz == null ? "" : clazz.getName());
		}
		return logger;
	}
}
