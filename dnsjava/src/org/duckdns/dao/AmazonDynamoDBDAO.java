package org.duckdns.dao;

import org.duckdns.dao.model.Domain;
import org.duckdns.utils.EnvironmentUtils;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class AmazonDynamoDBDAO {
	
	private static AmazonDynamoDBDAO instance = null;
	private static AmazonDynamoDBClient dynamoDB;
	private static DynamoDBMapper mapper;
	
	
	protected AmazonDynamoDBDAO() {
		dynamoDB = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        
        String local_db = EnvironmentUtils.getInstance().getLocal_db();
        if (local_db != null && local_db.trim().length() > 0) {
        	// LOCAL
        	dynamoDB.setEndpoint(local_db,"local","us-west-2"); 
        } else {
        	// PROD
        	dynamoDB.setRegion(usWest2);
        }
        mapper = new DynamoDBMapper(dynamoDB);
	}
	
	public static AmazonDynamoDBDAO getInstance() {
		if(instance == null) {
			instance = new AmazonDynamoDBDAO();
		}
		return instance;
	}
	
	public static String getSubdomain(String domainName) {
		int posOfPattern = domainName.indexOf(".duckdns.org");
		if (posOfPattern != -1) {
			domainName = domainName.substring(0,posOfPattern);
			
			int lastDot = domainName.lastIndexOf('.');
			if (lastDot > -1) {
				return domainName.substring(lastDot+1);
			} else {
				return domainName;
			}
		}
		return null;
	}

	public Domain domainGetDomain(String subDomain) {
		// Query comes already as a Subdomain
		if (subDomain != null) {
			System.out.println("***** LOOKUP " + subDomain);
			Domain domain = mapper.load(Domain.class, subDomain);
			return domain;
		}
		return null;
	}
	
}
