package org.duckdns.dao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.duckdns.dao.model.Account;
import org.duckdns.dao.model.Domain;
import org.duckdns.dao.model.Session;
import org.duckdns.util.EnvironmentUtils;
import org.duckdns.util.ValidationUtils;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;


public class AmazonDynamoDBDAO {
	
	private static AmazonDynamoDBDAO instance = null;
	private static AmazonDynamoDBClient dynamoDB;
	private static DynamoDBMapper mapper;
	
	private static final String SIMPLE_PATTERN = "MM/dd/yy hh:mm:ss aa";
	
	private static final Log LOG = LogFactory.getLog(AmazonDynamoDBDAO.class);
	
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
	
	public Session sessionsCreateSession(String jsessionId, String email, String ip) {
		Session session = new Session();
		session.setJsessionId(jsessionId);
		session.setEmail(email);
		session.setIp(ip);
		session.setLastHit(System.currentTimeMillis());
		mapper.save(session);
		LOG.info("Sessions Create Result: " + session);
		return session;
	}
	
	public boolean sessionsDeleteSession(String jsessionId, String ip) {
		Session session = sessionsGetSession(jsessionId, ip);
		if (session != null) {
			mapper.delete(session);
			return true;
		}
		return false;
	}
	
	public boolean sessionsRefreshSession(String jsessionId, String ip) {
		Session session = sessionsGetSession(jsessionId, ip);
		if (session != null) {
			session.setLastHit(System.currentTimeMillis());
			mapper.save(session);
			return true;
		}
		return false;
	}
	
	public Session sessionsGetSession(String jsessionId, String ip) {
		if (jsessionId == null || ip == null) {
			return null;
		}
		Session session = mapper.load(Session.class, jsessionId);
		if (session == null) {
			return null;
		}
		// SECURITY CHECK
		if (session.getIp() != null && session.getIp().equals(ip)) {
			LOG.info("Session Get Result: " + session);           
			return session;
		}
		return null;
	}
	
	public void sessionsCleanUpOldSessions() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		scanExpression.addFilterCondition("lastHit", 
				new Condition()
        			.withComparisonOperator(ComparisonOperator.LT.toString())
        			.withAttributeValueList(new AttributeValue().withN(""+(System.currentTimeMillis()-(Session.TTL_SESSIONS_SECONDS * 1000))))
				);
		PaginatedScanList<Session> result = mapper.scan(Session.class, scanExpression);
		mapper.batchDelete(result);
	}
	
	public Account accountsCreateAccount(String email) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_PATTERN);
		
		Account account = new Account();
		account.setEmail(email);
		account.setAccountType(Account.ACCOUNT_FREE);
		account.setAccountToken(UUID.randomUUID().toString());
		account.setLastUpdated(simpleDateFormat.format(new Date()));
		account.setCreatedDate(simpleDateFormat.format(new Date()));
		mapper.save(account);
		// LOG.info("Accounts Create Result: " + account);
		return account;
	}
	
	public Account lockAccount(String email) {
		Account account = mapper.load(Account.class, email);
		if (account != null) {
			if (!account.getAccountType().equals(Account.ACCOUNT_SUPER_DUCK)) {
				account.setAccountType(Account.ACCOUNT_NAUGHTY_DUCK);
			}
			mapper.save(account);
			// NOW SET ALL DOMAINS TO BE LOCKED
			List<Domain> domains = domainsGetDomainsByToken(account.getAccountToken());
			for (Domain domain : domains) {
				domain.setLocked("true");
				mapper.save(domain);
			}
		}
		return account;
	}
	
	public Account unlockAccount(String email) {
		Account account = mapper.load(Account.class, email);
		if (account != null) {
			if (account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
				account.setAccountType(Account.ACCOUNT_FREE);
			}
			mapper.save(account);
			// NOW SET ALL DOMAINS TO BE LOCKED
			List<Domain> domains = domainsGetDomainsByToken(account.getAccountToken());
			for (Domain domain : domains) {
				domain.setLocked("false");
				mapper.save(domain);
			}
		}
		return account;
	}
	
	public Account accountUpgradeAccount(String email) {
		Account account = mapper.load(Account.class, email);
		if (account != null) {
			if (account.getAccountType().equals(Account.ACCOUNT_FREE)) {
				account.setAccountType(Account.ACCOUNT_DONATE);
			}
			mapper.save(account);
		}
		return account;
	}
	
	public Account accountDowngradeAccount(String email) {
		Account account = mapper.load(Account.class, email);
		if (account != null) {
			if (account.getAccountType().equals(Account.ACCOUNT_DONATE)) {
				account.setAccountType(Account.ACCOUNT_FREE);
			}
			mapper.save(account);
		}
		return account;
	}
	
	public PaginatedScanList<Account> getAllAccounts() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		PaginatedScanList<Account> accounts = mapper.scan(Account.class,scanExpression);
		return accounts;
	}
	
	public PaginatedScanList<Account> getAllDonatedAccounts() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		scanExpression.addFilterCondition("accountType",new Condition()
		.withComparisonOperator(ComparisonOperator.EQ.toString())
		.withAttributeValueList(new AttributeValue[] { new AttributeValue().withS(Account.ACCOUNT_DONATE) }));
		PaginatedScanList<Account> accounts = mapper.scan(Account.class,scanExpression);
		return accounts;
	}
	
	public PaginatedScanList<Account> getAllFriendsAccounts() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		scanExpression.addFilterCondition("accountType",new Condition()
		.withComparisonOperator(ComparisonOperator.EQ.toString())
		.withAttributeValueList(new AttributeValue[] { new AttributeValue().withS(Account.ACCOUNT_FRIENDS_OF_DUCK) }));
		PaginatedScanList<Account> accounts = mapper.scan(Account.class,scanExpression);
		return accounts;
	}
	
	public PaginatedScanList<Account> getAllRedditAccounts() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		scanExpression.addFilterCondition("email",new Condition()
		.withComparisonOperator(ComparisonOperator.CONTAINS.toString())
		.withAttributeValueList(new AttributeValue[] { new AttributeValue().withS("@reddit") }));
		PaginatedScanList<Account> accounts = mapper.scan(Account.class,scanExpression);
		return accounts;
	}
	
	public PaginatedScanList<Account> getAllFacebookAccounts() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		scanExpression.addFilterCondition("email",new Condition()
		.withComparisonOperator(ComparisonOperator.CONTAINS.toString())
		.withAttributeValueList(new AttributeValue[] { new AttributeValue().withS("#facebook") }));
		PaginatedScanList<Account> accounts = mapper.scan(Account.class,scanExpression);
		return accounts;
	}
	
	public Account accountFindAccountByDomain(String domainName) {
		Domain domain = (Domain) mapper.load(Domain.class, domainName);
		if (domain != null) {
			return accountGetAccountByToken(domain.getAccountToken());
		}
		return null;
	}
	
	public Account accountGetAccountByToken(String token) {
		
		Condition hashKeyCondition = new Condition();
		hashKeyCondition.withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(new AttributeValue().withS(token));

		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put("accountToken", hashKeyCondition);

		QueryRequest queryRequest = new QueryRequest();
		queryRequest.withTableName("accountsv2");
		queryRequest.withIndexName("accountToken-index");
		queryRequest.withKeyConditions(keyConditions);

		QueryResult result = dynamoDB.query(queryRequest);

		for(Map<String, AttributeValue> item : result.getItems()) {
			Account mappedItem = mapper.marshallIntoObject(Account.class, item);
			// Only want the First one
			return mappedItem;
		}
		
		return null;
	}

	public Account accountsGetAccount(String email, boolean forceCreate) {
		Account account = mapper.load(Account.class, email);
		if (account == null) {
			if (!forceCreate) {
				return null;
			}
			account = accountsCreateAccount(email);
			//GoogleAnalyticsHelper.RecordAsyncEvent(UUID.randomUUID().toString(),GoogleAnalyticsHelper.CATEGORY_ACCOUNT,GoogleAnalyticsHelper.ACTION_CREATE,"","");
		}
		boolean hadToFixSomething = false;
		if (account.getAccountToken() == null || account.getAccountToken().length() == 0) {
			// DEAL WITH ACCOUNTS WITH NO TOKEN
			account.setAccountToken(UUID.randomUUID().toString());
			hadToFixSomething = true;
		}
		if (account.getAccountType() == null || account.getAccountType().length() == 0) {
			// DEAL WITH ACCOUNTS WITH NO TYPE
			account.setAccountType(Account.ACCOUNT_FREE);
			hadToFixSomething = true;
		}
		if (account.getCreatedDate() == null || account.getCreatedDate().length() == 0) {
			// DEAL WITH BLANK CREATED DATE
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_PATTERN);
			account.setCreatedDate(simpleDateFormat.format(new Date()));
			hadToFixSomething = true;
		}
		if (hadToFixSomething) {
			mapper.save(account);
		}
		// LOG.info("Account Get Result: " + account);           
		return account;
	}
	
	public Account accountsCreateAccount(String email, String accountToken, String lastUpdated, String createdDate, String accountType) {
		Account account = new Account();
		account.setEmail(email);
		account.setAccountToken(accountToken);
		account.setLastUpdated(lastUpdated);
		account.setCreatedDate(createdDate);
		account.setAccountType(accountType);
		mapper.save(account);
		// LOG.info("Account Create Result: " + account);
		return account;
	}
	
	public Domain domainsCreateDomain(String domainName, String accountToken, String currentIp) {
		
		if (!ValidationUtils.isValidSubDomain(domainName)) {
			return null;
		}
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_PATTERN);
		
		Domain domain = new Domain();
		domain.setDomainName(domainName);
		domain.setAccountToken(accountToken);
		if (currentIp != null && currentIp.indexOf(":") > -1) {
			domain.setCurrentIpV6(currentIp);
		} else {
			domain.setCurrentIp(currentIp);
		}
		domain.setLastUpdated(simpleDateFormat.format(new Date()));
		mapper.save(domain);
		// LOG.info("Domains Create Result: " + domain);
		return domain;
	}
	
	public Domain domainsCreateDomain(String domainName, String accountToken, String currentIp, String currentIpV6, String lastUpdated) {
		
		if (!ValidationUtils.isValidSubDomain(domainName)) {
			return null;
		}
		
		Domain domain = new Domain();
		domain.setDomainName(domainName);
		domain.setAccountToken(accountToken);
		domain.setCurrentIp(currentIp);
		domain.setCurrentIpV6(currentIpV6);
		domain.setLastUpdated(lastUpdated);
		mapper.save(domain);
		// LOG.info("Domains Create Result: " + domain);
		return domain;
	}
	
	public PaginatedScanList<Domain> getAllDomains() {
		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		PaginatedScanList<Domain> domains = mapper.scan(Domain.class,scanExpression);
		return domains;
	}

	public Domain domainGetDomain(String domainName) {
		Domain domain = mapper.load(Domain.class, domainName);
		// LOG.info("Domains Get Result: " + domain);           
		return domain;
	}
	
	public boolean domainUpdateIp(String ipAddress, String domainName, String accountToken) {
		
		if (!ValidationUtils.isValidSubDomain(domainName)) {
			return false;
		}
		if (!ValidationUtils.isValidIpAddress(ipAddress)) {
			return false;
		}
		
		Domain domain = mapper.load(Domain.class, domainName);
		if (domain == null) {
			return false;
		}
		LOG.info("Domains update ip check Result: " + domain); 
		if (domain.getAccountToken().equals(accountToken)) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_PATTERN);
			domain.setCurrentIp(ipAddress);
			domain.setLastUpdated(simpleDateFormat.format(new Date()));
			mapper.save(domain);
			return true;
		}
		return false;
	}
	
	public boolean domainDeleteDomain(String domainName, String accountToken) {
		Domain domain = mapper.load(Domain.class, domainName);
		if (domain == null) {
			return false;
		}
		LOG.info("Domains Delete check Result: " + domain); 
		if (domain.getAccountToken().equals(accountToken)) {
			mapper.delete(domain);
			return true;
		}
		return false;
	}
	
	public String recreateToken(String email) {
		// GET OLD TOKEN
		Account currentAccount = accountsGetAccount(email, false);
		String oldToken = currentAccount.getAccountToken();
		
		// GENERATE NEW TOKEN
		String newToken = UUID.randomUUID().toString();
		
		// UPDATE THE ACCOUNT TABLE
		currentAccount.setAccountToken(newToken);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_PATTERN);
		currentAccount.setLastUpdated(simpleDateFormat.format(new Date()));
		mapper.save(currentAccount);
        
		// SEARCH FOR MATCHING DOMAIN RECORD
		List<Domain> domains = domainsGetDomainsByToken(oldToken);
        
        // SPIN THROUGH THEM UPDATING THEM
        for (Domain domain : domains) {
        	domain.setAccountToken(newToken);
        	mapper.save(domain);
        }
        
        return newToken;
	}

	public List<Domain> domainsGetDomainsByToken(String token) {
		
		Condition hashKeyCondition = new Condition();
		hashKeyCondition.withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(new AttributeValue().withS(token));


		Map<String, Condition> keyConditions = new HashMap<String, Condition>();
		keyConditions.put("accountToken", hashKeyCondition);


		QueryRequest queryRequest = new QueryRequest();
		queryRequest.withTableName("domainsv2");
		queryRequest.withIndexName("accountToken-index");
		queryRequest.withKeyConditions(keyConditions);

		QueryResult result = dynamoDB.query(queryRequest);
		
		List<Domain> mappedItems = new ArrayList<Domain>();

		for(Map<String, AttributeValue> item : result.getItems()) {
			Domain mappedItem = mapper.marshallIntoObject(Domain.class, item);
		    mappedItems.add(mappedItem);
		}
		
		return mappedItems;
	}
	
	public boolean accountDeleteAccountLeaveDomains(String email) {
		Account account = accountsGetAccount(email, false);
		if (account != null) {
			mapper.delete(account);
		} else {
			return false;
		}
		return true;
	}

	public boolean accountDeleteAccount(String email) {
		Account account = accountsGetAccount(email, false);
		if (account != null) {
			List<Domain> domains = domainsGetDomainsByToken(account.getAccountToken());
			if (domains != null) {
				for (Domain domain : domains) {
					mapper.delete(domain);
				}
			}
			mapper.delete(account);
		} else {
			return false;
		}
		return true;
	}
}
