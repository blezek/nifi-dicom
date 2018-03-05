/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

public class MessageServiceElementCommand {
	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/network/MessageServiceElementCommand.java,v 1.5 2017/01/24 10:50:45 dclunie Exp $";

	public static final int NOCOMMAND = 0x0000;	// used as a flag
	
	public static final int C_STORE_RQ = 0x0001;
	public static final int C_STORE_RSP = 0x8001;
	public static final int C_GET_RQ = 0x0010;
	public static final int C_GET_RSP = 0x8010;
	public static final int C_FIND_RQ = 0x0020;
	public static final int C_FIND_RSP = 0x8020;
	public static final int C_MOVE_RQ = 0x0021;
	public static final int C_MOVE_RSP = 0x8021;
	public static final int C_ECHO_RQ = 0x0030;
	public static final int C_ECHO_RSP = 0x8030;
	public static final int N_EVENT_REPORT_RQ = 0x0100;
	public static final int N_EVENT_REPORT_RSP = 0x8100;
	public static final int N_GET_RQ = 0x0110;
	public static final int N_GET_RSP = 0x8110;
	public static final int N_SET_RQ = 0x0120;
	public static final int N_SET_RSP = 0x8120;
	public static final int N_ACTION_RQ = 0x0130;
	public static final int N_ACTION_RSP = 0x8130;
	public static final int N_CREATE_RQ = 0x0140;
	public static final int N_CREATE_RSP = 0x8140;
	public static final int N_DELETE_RQ = 0x0150;
	public static final int N_DELETE_RSP = 0x8150;
	public static final int C_CANCEL_RQ = 0x0FFF;
	
	/***/
	public static final String toString(int command) {
		String s;
		switch (command) {
			case C_STORE_RQ:		s="C-STORE-RQ"; break;
			case C_STORE_RSP:		s="C-STORE-RSP"; break;
			case C_GET_RQ:			s="C-GET-RQ"; break;
			case C_GET_RSP:			s="C-GET-RSP"; break;
			case C_FIND_RQ:			s="C-FIND-RQ"; break;
			case C_FIND_RSP:		s="C-FIND-RSP"; break;
			case C_MOVE_RQ:			s="C-MOVE-RQ"; break;
			case C_MOVE_RSP:		s="C-MOVE-RSP"; break;
			case C_ECHO_RQ:			s="C-ECHO-RQ"; break;
			case C_ECHO_RSP:		s="C-ECHO-RSP"; break;
			case N_EVENT_REPORT_RQ:		s="N-EVENT-REPORT-RQ"; break;
			case N_EVENT_REPORT_RSP:	s="N-EVENT-REPORT-RSP"; break;
			case N_GET_RQ:			s="N-GET-RQ"; break;
			case N_GET_RSP:			s="N-GET-RSP"; break;
			case N_SET_RQ:			s="N-SET-RQ"; break;
			case N_SET_RSP:			s="N-SET-RSP"; break;
			case N_ACTION_RQ:		s="N-ACTION-RQ"; break;
			case N_ACTION_RSP:		s="N-ACTION-RSP"; break;
			case N_CREATE_RQ:		s="N-CREATE-RQ"; break;
			case N_CREATE_RSP:		s="N-CREATE-RSP"; break;
			case N_DELETE_RQ:		s="N-DELETE-RQ"; break;
			case N_DELETE_RSP:		s="N-DELETE-RSP"; break;
			case C_CANCEL_RQ:		s="C-CANCEL-RQ"; break;
			default:			s="--UNKNOWN--"; break;
		}
		return s;
	}
}
