package io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton for all console output
 * 
 * @author Fabian Braun
 * 
 */
public class Log {

	private static Log instance;
	private static final String CONFIG_FILE = "configuration.properties";
	private boolean enabled;

	/** Don't let anyone else instantiate this class */
	private Log() {
		enabled = true;
		// read configuration
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(CONFIG_FILE);
			prop.load(input);
			if ("false".equals(prop.getProperty("debug"))) {
				enabled = false;
			}
			info("debug log is " + (enabled ? "enabled" : "disabled"));
		} catch (IOException ex) {
			error("config " + CONFIG_FILE
					+ " file can not be read! Verbose logging is enabled");
			enabled = true;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 
	 * @return the application wide unique <code>Log</code> object
	 */
	public static synchronized Log getInstance() {
		if (instance == null) {
			instance = new Log();
		}
		return instance;
	}

	/**
	 * if logging is enabled msg will be printed to System.out
	 * 
	 * @param msg
	 */
	public void debug(String msg) {
		if (enabled)
			System.out.println(msg);
	}

	/**
	 * msg will be printed to System.out
	 * 
	 * @param msg
	 */
	public void info(String msg) {
		System.out.println(msg);
	}

	/**
	 * msg will be printed to System.err
	 * 
	 * @param msg
	 */
	public void error(String msg) {
		System.err.println(msg);
	}

	/**
	 * msg and e.getMessage() will be printed to System.err if logging is
	 * enabled also the Stacktrace of e will be printed to System.err
	 * 
	 * @param msg
	 * @param e
	 */
	public void error(String msg, Exception e) {
		System.err.println(msg);
		if (enabled)
			e.printStackTrace();
		else
			System.err.println("Exception message: " + e.getMessage());
	}

	/**
	 * e.getMessage() will be printed to System.err if logging is enabled also
	 * the Stacktrace of e will be printed to System.err
	 * 
	 * @param e
	 */
	public void error(Exception e) {
		if (enabled)
			e.printStackTrace();
		else
			System.err.println(e.getMessage());
	}

	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}
}
