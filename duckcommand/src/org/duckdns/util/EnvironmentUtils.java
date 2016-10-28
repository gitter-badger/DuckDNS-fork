package org.duckdns.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EnvironmentUtils {

	public static final String OUR_DOMAIN = ".duckdns.org";
	public static final String EXPECTED_DOMAIN = "www.duckdns.org";
	
	private String FIXED_IP_NS1 = "";
	private String FIXED_IP_NS2 = "";
	private String FIXED_IP_NS3 = "";
	
	private String FIXED_IP_NS1_INTERNAL = "";
	private String FIXED_IP_NS2_INTERNAL = "";
	private String FIXED_IP_NS3_INTERNAL = "";
	
	private static EnvironmentUtils instance = null;
	
	public static EnvironmentUtils getInstance() {
		if (instance == null) {
			instance = new EnvironmentUtils();
		}
		return instance;
	}
	
	private String public_hostname;
	private String dynamo_session_cache;
	private String local_db;
	
	protected EnvironmentUtils() {
		// LOAD THE EXTERNAL RESOURCES FROM 
		// jetty/resources/
		Properties prop = new Properties();
		try {
			InputStream in = EnvironmentUtils.class.getClassLoader().getResource("environment.properties").openStream();
			prop.load(in);
			in.close();
		} catch (IOException io) {
		}
		public_hostname = prop.getProperty("public-hostname", "did not work");
		dynamo_session_cache = prop.getProperty("dynamo-session-cache", "false");
		local_db = prop.getProperty("local-db", "did not work");
		
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
		FIXED_IP_NS1=secretProp.getProperty("FIXED_IP_NS1", "did not work");
		FIXED_IP_NS2=secretProp.getProperty("FIXED_IP_NS2", "did not work");
		FIXED_IP_NS3=secretProp.getProperty("FIXED_IP_NS3", "did not work");
		FIXED_IP_NS1_INTERNAL=secretProp.getProperty("FIXED_IP_NS1_INTERNAL", "did not work");
		FIXED_IP_NS2_INTERNAL=secretProp.getProperty("FIXED_IP_NS2_INTERNAL", "did not work");
		FIXED_IP_NS3_INTERNAL=secretProp.getProperty("FIXED_IP_NS3_INTERNAL", "did not work");
	}
	
	public String getLocal_db() {
		return local_db;
	}

	public static String getHostName() {
		return EnvironmentUtils.getInstance().public_hostname;
	}
	
	public static boolean isProduction() {
		if (EnvironmentUtils.getInstance().public_hostname.equals(EXPECTED_DOMAIN)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getProtocol() {
		if (isProduction()) {
			return "https";
		} else {
			return "http";
		}
	}
	
	public static boolean isDynamoSessionCache() {
		if (EnvironmentUtils.getInstance().dynamo_session_cache.equals("true")) {
			return true;
		}
		return false;
	}
	
	public String getFIXED_IP_NS1() {
		return FIXED_IP_NS1;
	}

	public String getFIXED_IP_NS2() {
		return FIXED_IP_NS2;
	}

	public String getFIXED_IP_NS3() {
		return FIXED_IP_NS3;
	}

	public String getFIXED_IP_NS1_INTERNAL() {
		return FIXED_IP_NS1_INTERNAL;
	}

	public String getFIXED_IP_NS2_INTERNAL() {
		return FIXED_IP_NS2_INTERNAL;
	}

	public String getFIXED_IP_NS3_INTERNAL() {
		return FIXED_IP_NS3_INTERNAL;
	}
	
}

