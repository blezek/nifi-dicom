/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.dicom.VersionAndConstants;
import com.pixelmed.utils.FileUtilities;

import java.awt.*; 
import java.awt.event.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*; 

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

//import com.pixelmed.display.event.*; 

/**
 * <p>This class provides the infrastructure for creating applications (which extend
 * this class) and provides them with utilities for creating a main window with a
 * title and default close and dispose behavior, as well as access to properties,
 * and a window snapshot function.</p>
 * 
 * @author	dclunie
 */
public class ApplicationFrame extends JFrame {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/ApplicationFrame.java,v 1.40 2017/01/24 10:50:40 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ApplicationFrame.class);

	protected static String resourceBundleName  = "com.pixelmed.display.ApplicationFrame";
	
	protected static ResourceBundle resourceBundle;
	
	protected static void localizeJOptionPane() {
		if (resourceBundle == null) {
			try {
				resourceBundle = ResourceBundle.getBundle(resourceBundleName);
				for (Enumeration<String> e = resourceBundle.getKeys(); e.hasMoreElements();) {
					String key = e.nextElement();
					if (key.startsWith("OptionPane.")) {
						String value = resourceBundle.getString(key);
						slf4jlogger.debug("localizeJOptionPane(): UIManager.put(\"{}\" , \"{}\")",key,value);
						UIManager.put(key,value);
					}
				}
			}
			catch (Exception e) {
				// ignore java.util.MissingResourceException: Can't find bundle for base name com.pixelmed.display.ApplicationFrame, locale en_US
				slf4jlogger.warn("Missing resource bundle for localization {}",e.toString());
			}
		}
	}
	
	
	/**
	 * <p>Get the release string for this application.</p>
	 *
	 * @return	 the release string
	 */
	protected static String getReleaseString() {
		return VersionAndConstants.releaseString;
	}

	/**
	 * <p>Get the date the package was built.</p>
	 *
	 * @return	 the build date
	 */
	protected static String getBuildDate() {
		return VersionAndConstants.getBuildDate();
	}
	
	protected StatusBarManager statusBarManager;		// maintain a strong reference else weak reference to listener gets nulled when garbage collected

	/**
	 * <p>Setup a StatusBarManager and return its StatusBar.</p>
	 *
	 * <p>The initial string in the StatusBar is composed of the build date and release string.</p>
	 *
	 * @return	 the StatusBar
	 */
	protected JLabel getStatusBar() {
		statusBarManager = new StatusBarManager(getBuildDate()+" "+getReleaseString());		// maintain a strong reference else weak reference to listener gets nulled when garbage collected
		return statusBarManager.getStatusBar();
	}

	/**
	 * <p>Given a file name, such as the properties file name, make a path to it in the user's home directory.</p>
	 *
	 * @param	fileName	 the file name to make a path to
	 */
	protected static String makePathToFileInUsersHomeDirectory(String fileName) {
		return FileUtilities.makePathToFileInUsersHomeDirectory(fileName);
	}
	
	private Properties applicationProperties;
	private String applicationPropertyFileName;
	
	/**
	 * <p>Store the properties from the current properties file.</p>
	 */
	protected void loadProperties() {
		applicationProperties = new Properties(/*defaultProperties*/) {
			// keep them sorted alphabetically, per http://stackoverflow.com/questions/17011108/how-can-i-write-java-properties-in-a-defined-order
			public synchronized Enumeration<Object> keys() {
				return Collections.enumeration(new TreeSet<Object>(super.keySet()));
			}
		};
		if (applicationPropertyFileName != null) {
			String whereFrom = makePathToFileInUsersHomeDirectory(applicationPropertyFileName);
			try {
				// load properties from last invocation
				FileInputStream in = new FileInputStream(whereFrom);
				applicationProperties.load(in);
				in.close();
			}
			catch (IOException e) {
				System.err.println(e);
			}
		}
	}
	
	/**
	 * <p>Store the current properties in the current properties file.</p>
	 *
	 * @param	comment		the description to store as the header of the properties file
	 * @throws	IOException
	 */
	protected void storeProperties(String comment) throws IOException {
		if (applicationPropertyFileName == null) {
			throw new IOException("asked to store properties but no applicationPropertyFileName was ever set");
		}
		else {
			String whereTo = makePathToFileInUsersHomeDirectory(applicationPropertyFileName);
			FileOutputStream out = new FileOutputStream(whereTo);
			applicationProperties.store(out,comment);
			out.close();
		}
	}
	
	/**
	 * <p>Get the properties for the application that have already been loaded (see {@link #loadProperties() loadProperties()}).</p>
	 *
	 * @return	the properties
	 */
	protected Properties getProperties() { return applicationProperties; }
	
	/**
	 * <p>Get the name of the property file set for the application.</p>
	 *
	 * @return	the property file name
	 */
	protected String getApplicationPropertyFileName () { return applicationPropertyFileName; }
	
	/**
	 * <p>Set the name of the property file set for the application.</p>
	 *
	 * @param	applicationPropertyFileName	the property file name
	 */
	protected void setApplicationPropertyFileName (String applicationPropertyFileName) { this.applicationPropertyFileName=applicationPropertyFileName; }
	
	/**
	 * <p>Searches for the property with the specified key in the specified property list, insisting on a value.</p>
	 *
	 * @param	properties	the property list to search
	 * @param	key		the property name
	 * @throws	Exception	if there is no such property or it has no value
	 */
	static public String getPropertyInsistently(Properties properties,String key) throws Exception {
		String value = properties.getProperty(key);
		if (value == null || value.length() == 0) {
			throw new Exception("Properties do not contain value for "+key);
		}
		return value;
	} 

	/**
	 * <p>Searches for the property with the specified key in this application's property list, insisting on a value.</p>
	 *
	 * @param	key		the property name
	 * @throws	Exception	if there is no such property or it has no value
	 */
	public String getPropertyInsistently(String key) throws Exception {
		return getPropertyInsistently(applicationProperties,key);
	} 

	/**
	 * <p>Get the value of a property from the specified property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to he specified property list if not already present.</p>
	 *
	 * @param	properties		the property list to search
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	static public String getPropertyOrDefaultAndAddIt(Properties properties,String key,String defaultValue) {
		String propertyValue = defaultValue;
		{
			String propertyStringValue = properties.getProperty(key);
			if (propertyStringValue == null) {
				properties.setProperty(key,propertyValue);
			}
			else {
				propertyValue=propertyStringValue;
			}
		}
		return propertyValue;
	}

	/**
	 * <p>Get the value of a property from this application's property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to this application's property list if not already present.</p>
	 *
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	public String getPropertyOrDefaultAndAddIt(String key,String defaultValue) {
		return getPropertyOrDefaultAndAddIt(applicationProperties,key,defaultValue);
	}

	/**
	 * <p>Get the value of a boolean property from the specified property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to he specified property list if not already present.</p>
	 *
	 * @param	properties		the property list to search
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	static public boolean getBooleanPropertyOrDefaultAndAddIt(Properties properties,String key,boolean defaultValue) {
		boolean propertyValue = defaultValue;
		{
			String propertyStringValue = properties.getProperty(key);
			if (propertyStringValue == null) {
				properties.setProperty(key,Boolean.toString(propertyValue));
			}
			else {
				propertyValue=Boolean.parseBoolean(propertyStringValue);
			}
		}
		return propertyValue;
	}

	/**
	 * <p>Get the value of a boolean property from this application's property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to this application's property list if not already present.</p>
	 *
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	public boolean getBooleanPropertyOrDefaultAndAddIt(String key,boolean defaultValue) {
		return getBooleanPropertyOrDefaultAndAddIt(applicationProperties,key,defaultValue);
	}

	/**
	 * <p>Get the value of an integer property from the specified property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to he specified property list if not already present.</p>
	 *
	 * @param	properties		the property list to search
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	static public int getIntegerPropertyOrDefaultAndAddIt(Properties properties,String key,int defaultValue) {
		int propertyValue = defaultValue;
		{
			String propertyStringValue = properties.getProperty(key);
			if (propertyStringValue == null) {
				properties.setProperty(key,Integer.toString(propertyValue));
			}
			else {
				try {
					propertyValue=Integer.parseInt(propertyStringValue);
				}
				catch (NumberFormatException e) {
					slf4jlogger.error("", e);
					// leave as default value
				}
			}
		}
		return propertyValue;
	}

	/**
	 * <p>Get the value of an integer property from this application's property list or a default, adding it.</p>
	 *
	 * <p>Adds the default property to this application's property list if not already present.</p>
	 *
	 * @param	key				the property name
	 * @param	defaultValue	the value to use if absent
	 */
	public int getIntegerPropertyOrDefaultAndAddIt(String key,int defaultValue) {
		return getIntegerPropertyOrDefaultAndAddIt(applicationProperties,key,defaultValue);
	}
	
	/**
	 * <p>Store a JPEG snapshot of the specified window in the user's home directory.</p>
	 *
	 * @param	extent		the rectangle to take a snapshot of (typically <code>this.getBounds()</code> for whole application)
	 */
	protected File takeSnapShot(Rectangle extent) {
		File snapShotFile = null;
		try {
			snapShotFile = File.createTempFile("snap",".jpg",new File(System.getProperty("user.home")));
			java.awt.image.BufferedImage snapShotImage = new Robot().createScreenCapture(extent);
			javax.imageio.ImageIO.write(snapShotImage,"jpeg",snapShotFile);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return snapShotFile;
	}
	
	/**
	 * <p>Construct a window with the default size and title and no property source.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * <p>Will exit the application when the window closes.</p>
	 */
	public ApplicationFrame() {
		//this("Application Frame",640,480); 
		this("Application Frame",null);
	} 
 
	/**
	 * <p>Construct a window with the default size and title and no property source.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * @param	closeOperation	argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public ApplicationFrame(int closeOperation) {
		//this("Application Frame",640,480,closeOperation); 
		this("Application Frame",null,closeOperation);
	} 
 
	/**
	 * <p>Construct a window with the default size, specified title and no property source.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * <p>Will exit the application when the window closes.</p>
	 *
	 * @param	title				the title for the top bar decoration
	 */
	public ApplicationFrame(String title) {
		//this(title,640,480); 
		this(title,null);
	} 
 
	/**
	 * <p>Construct a window with the default size, specified title and no property source.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * @param	title					the title for the top bar decoration
	 * @param	closeOperation			argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public ApplicationFrame(String title,int closeOperation) {
		//this(title,640,480,closeOperation); 
		this(title,null,closeOperation);
	} 
 
	/**
	 * <p>Construct a window with the default size, and specified title and property sources.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * <p>Will exit the application when the window closes.</p>
	 *
	 * @param	title						the title for the top bar decoration
	 * @param	applicationPropertyFileName	the name of the properties file
	 */
	public ApplicationFrame(String title,String applicationPropertyFileName) {
		this(title,applicationPropertyFileName,JFrame.EXIT_ON_CLOSE);
	} 
 
	/**
	 * <p>Construct a window with the default size, and specified title and property sources.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * @param	title						the title for the top bar decoration
	 * @param	applicationPropertyFileName	the name of the properties file
	 * @param	closeOperation				argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public ApplicationFrame(String title,String applicationPropertyFileName,int closeOperation) {
		setApplicationPropertyFileName(applicationPropertyFileName);
		loadProperties();
		if (title != null) setTitle(title); 
		createGUI(); 
		//setSize(640,480);
		setDefaultCloseOperation(closeOperation);
	} 

	/**
	 * <p>Construct a window with the specified size, title and property sources.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * <p>Will exit the application when the window closes.</p>
	 *
	 * @param	title						the title for the top bar decoration
	 * @param	applicationPropertyFileName	the name of the properties file
	 * @param	w							width
	 * @param	h							height
	 */
	public ApplicationFrame(String title,String applicationPropertyFileName,int w,int h) {
		this(title,applicationPropertyFileName,w,h,JFrame.EXIT_ON_CLOSE);
	} 

	/**
	 * <p>Construct a window with the specified size, title and property sources.</p>
	 *
	 * <p>Does not show the window.</p>
	 *
	 * @param	title						the title for the top bar decoration
	 * @param	applicationPropertyFileName	the name of the properties file
	 * @param	w							width
	 * @param	h							height
	 * @param	closeOperation				argument to {@link javax.swing.JFrame#setDefaultCloseOperation(int) setDefaultCloseOperation()}
	 */
	public ApplicationFrame(String title,String applicationPropertyFileName,int w,int h,int closeOperation) {
		setApplicationPropertyFileName(applicationPropertyFileName);
		loadProperties();
		if (title != null) setTitle(title); 
		createGUI(); 
		setSize(w,h);
		setDefaultCloseOperation(closeOperation);
	} 

	/**
	 * <p>Setup internationalized fonts if possible.</p>
	 *
	 * <p>Invoked by {@link com.pixelmed.display.ApplicationFrame#createGUI() createGUI()}.</p>
	 */
	public static void setInternationalizedFontsForGUI() {
		slf4jlogger.debug("ApplicationFrame.setInternationalizedFontsForGUI()");

		Font font = new Font("Arial Unicode MS",Font.PLAIN,12);
		if (font == null || !font.getFamily().equals("Arial Unicode MS")) {
			font = new Font("Bitstream Cyberbit",Font.PLAIN,13);
			if (font == null || !font.getFamily().equals("Bitstream Cyberbit")) {
				font=null;
			}
		}
		if (font == null) {
			System.err.println("Warning: couldn't set internationalized font: non-Latin values may not display properly");
		}
		else {
			slf4jlogger.debug("Using internationalized font {}",font);
			UIManager.put("Tree.font",font);
			UIManager.put("Table.font",font);
			//UIManager.put("Label.font",font);
		}

	} 
	
	/**
	 * <p>Setup background for UI.</p>
	 *
	 * <p>Invoked by {@link com.pixelmed.display.ApplicationFrame#createGUI() createGUI()}.</p>
	 */
	public static void setBackgroundForGUI() {
		String laf = UIManager.getLookAndFeel().getClass().getName();
		slf4jlogger.debug("setBackgroundForGUI(): L&F is {}",laf);
		if (UIManager.getLookAndFeel().getClass().getName().equals("com.apple.laf.AquaLookAndFeel")) {
			// we want the darker gray than is the default
			// note that the JFrame.setBackground(Color.lightGray) that we used to use does not reliably propagate
			UIManager.put("Panel.background",Color.lightGray);
			UIManager.put("CheckBox.background",Color.lightGray);
			UIManager.put("SplitPane.background",Color.lightGray);
		}
	}
	
	/**
	 * <p>Setup preferred Look and Feel.</p>
	 *
	 * <p>Invoked by {@link com.pixelmed.display.ApplicationFrame#createGUI() createGUI()}.</p>
	 */
	public static void setPreferredLookAndFeelForPlatform() {
		try {
			String osName = System.getProperty("os.name");
			if (osName != null && osName.toLowerCase(java.util.Locale.US).startsWith("windows")) {	// see "http://lopica.sourceforge.net/os.html" for list of values
				slf4jlogger.debug("ApplicationFrame.setPreferredLookAndFeelForPlatform(): detected Windows - using Windows LAF");
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			}
		}
		catch (Exception e) {
			slf4jlogger.error("", e);
		}
	}
	
	/**
	 * <p>Do what is necessary to build an application window.</p>
	 *
	 * <p>Invoked by constructors.</p>
	 *
	 * <p>Sub-classes should call this if they do not use the super() constructors,
	 * but should NOT usually need to override it, but rather should
	 * override the methods that it calls.</p>
	 */
	protected void createGUI() {
		slf4jlogger.debug("ApplicationFrame.createGUI()");
		setPreferredLookAndFeelForPlatform();
		localizeJOptionPane();
		setBackgroundForGUI();
		setInternationalizedFontsForGUI();
	} 
	
	/**
	 * <p>For testing.</p>
	 *
	 * <p>Shows an empty default sized window.</p>
	 *
	 * @param	arg	ignored
	 */
	public static void main(String arg[]) {
		ApplicationFrame af = new ApplicationFrame(); 
		af.setVisible(true); 
	} 
 
}

