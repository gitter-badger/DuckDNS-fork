package org.duckdns.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentUtils {

	private String GOOGLE_TRACKING_ID = "";
	private String local_db;
	
	private static EnvironmentUtils instance = null;
	
	public static EnvironmentUtils getInstance() {
		if (instance == null) {
			instance = new EnvironmentUtils();
		}
		return instance;
	}
	
	protected EnvironmentUtils() {
		
		// LOAD THE EXTERNAL RESOURCES FROM 
		// secret/secrets.properties
		Properties secretProp = new Properties();
		try {
			InputStream in = EnvironmentUtils.class.getClassLoader().getResource("secrets.properties").openStream();
			secretProp.load(in);
			in.close();
		} catch (IOException io) {
			System.out.println(io);
		} catch (NullPointerException npe) {
			System.out.println("missing config :" + npe);
		}
		GOOGLE_TRACKING_ID=secretProp.getProperty("GOOGLE_TRACKING_ID", "did not work");
		local_db = secretProp.getProperty("local-db", "did not work");
	}

	public String getGOOGLE_TRACKING_ID() {
		return GOOGLE_TRACKING_ID;
	}
	
	public String getLocal_db() {
		return local_db;
	}
	
}