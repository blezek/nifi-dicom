/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

/**
 * <p>An interface to communicate log and status messages.</p>
 *
 * <p>Both methods must be implemented.</p>
 *
 * @author      dclunie
 */
public interface MessageLogger {

        /**
	 * <p>Append the supplied text to the log.</p>
	 *
         * @param       message		the (possibly multi-line) text to append to the log
         */
        public void send(String message);

        /**
	 * <p>Append the supplied text to the log, followed by a new line.</p>
	 *
         * @param       message		the (possibly multi-line) text to append to the log
         */
        public void sendLn(String message);
}

