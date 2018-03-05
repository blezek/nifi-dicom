/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

/**
 * <p>This class provides a description of the parameters of a remote FTP host.</p>
 *
 * @author	dclunie
 */
public class FTPRemoteHost {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/ftp/FTPRemoteHost.java,v 1.4 2017/01/24 10:50:43 dclunie Exp $";
	
	protected String host;
	protected String user;
	protected String password;
	protected String directory;
	protected FTPSecurityType security;
	
	public boolean equals(Object obj) {
		if (obj instanceof FTPRemoteHost) {
			FTPRemoteHost hostComp = (FTPRemoteHost)obj;
			return ((host == null && hostComp.getHost() == null) || host.equals(hostComp.getHost()))
				&& ((user == null && hostComp.getUser() == null) || user.equals(hostComp.getUser()))
				&& ((password == null && hostComp.getPassword() == null) || password.equals(hostComp.getPassword()))
				&& ((directory == null && hostComp.getDirectory() == null) || directory.equals(hostComp.getDirectory()))
				&& ((security == null && hostComp.getSecurity() == null) || security.equals(hostComp.getSecurity()))
				;
		}
		else {
			return super.equals(obj);
		}
	}
		
	public FTPRemoteHost() {
	}
		
	public FTPRemoteHost(FTPRemoteHost r) {
		this.host = r.host;
		this.user = r.user;
		this.password = r.password;
		this.directory = r.directory;
		this.security = r.security;
	}
	
	public FTPRemoteHost(String host,String user,String password,String directory,FTPSecurityType security) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.directory = directory;
		this.security = security;
	}
	
	public FTPRemoteHost(String host,String user,String password,String directory,String security) {
		this(host,user,password,directory,FTPSecurityType.selectFromDescription(security));
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	
	public void setSecurity(FTPSecurityType security) {
		this.security = security;
	}
	
	public void setSecurity(String security) {
		this.security = FTPSecurityType.selectFromDescription(security);
	}
		
	public final String getHost() { return host; }
	public final String getUser() { return user; }
	public final String getPassword() { return password; }
	public final String getDirectory() { return directory; }
	public final FTPSecurityType getSecurity() { return security; }

	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("host=");
		strbuf.append(host);
		strbuf.append(",user=");
		strbuf.append(user);
		strbuf.append(",password=");
		strbuf.append(password);
		strbuf.append(",directory=");
		strbuf.append(directory);
		strbuf.append(",security=");
		strbuf.append(security);
		return strbuf.toString();
	}
}

