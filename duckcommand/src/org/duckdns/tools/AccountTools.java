package org.duckdns.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

import org.duckdns.comms.Sender;
import org.duckdns.comms.clients.BasicSender;
import org.duckdns.comms.messages.SimpleOneLineMessage;
import org.duckdns.dao.AmazonDynamoDBDAO;
import org.duckdns.dao.model.Account;
import org.duckdns.dao.model.Domain;
import org.duckdns.util.EnvironmentUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class AccountTools {
	
	private static Sender s1;
	private static Sender s2;
	private static Sender s3;
	
	public static void main(String[] args) {
		s1 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS1_INTERNAL(), 10025, 3, 100, 2000, new BasicSender());
		s2 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS2_INTERNAL(), 10025, 3, 100, 2000, new BasicSender());
		s3 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS3_INTERNAL(), 10025, 3, 100, 2000, new BasicSender());
		
		if (args.length > 0) {
			String command = args[0];
			if (command.equals("findUser")) {
				if (args.length > 1) {
					String theLookupDomain = args[1];
					findUser(theLookupDomain);
					return;
				}
			} else if (command.equals("lockAccount")) {
				if (args.length > 1) {
					String theLookupDomain = args[1];
					lockAccount(theLookupDomain);
					return;
				}
			} else if (command.equals("unlockAccount")) {
				if (args.length > 1) {
					String theLookupDomain = args[1];
					unlockAccount(theLookupDomain);
					return;
				}
			}else if (command.equals("upgradeAccount")) {
				if (args.length > 1) {
					String email = args[1];
					upgradeAccount(email);
					return;
				}
			} else if (command.equals("downgradeAccount")) {
				if (args.length > 1) {
					String email = args[1];
					downgradeAccount(email);
					return;
				}
			} else if (command.equals("accountStats")) {
				accountStats();
				return;
			} else if (command.equals("domainStats")) {
				domainStats();
				return;
			} else if (command.equals("backupDomains")) {
				String fileLoc = args[1];
				backupDomains(fileLoc);
				return;
			} else if (command.equals("backupAccounts")) {
				String fileLoc = args[1];
				backupAccounts(fileLoc);
				return;
			} else if (command.equals("restoreDomains")) {
				String fileLoc = args[1];
				restoreDomains(fileLoc);
				return;
			} else if (command.equals("restoreAccounts")) {
				String fileLoc = args[1];
				restoreAccounts(fileLoc);
				return;
			} else if (command.equals("clearDomains")) {
				clearDomains();
				return;
			} else if (command.equals("clearAccounts")) {
				clearAccounts();
				return;
			} else if (command.equals("updateApp")) {
				updateApp();
				return;
			} 
		}
		System.out.println("quack V2 - valid commands are : \n findUser(domain)\n lockAccount(domain)\n unlockAccount(domain)\n accountStats\n domainStats\n upgradeAccount(email)\n downgradeAccount(email)\n backupDomains(file)\n backupAccounts(file)\n restoreDomains(file)\n restoreAccounts(file)\n clearDomains\n clearAccounts\n updateApp\n");
	}
	
	private static void upgradeAccount(String email) {
		if (email.length() > 0) {
			System.out.println("-----------------");
			System.out.println("Looking up : " + email);
			System.out.println("-----------------");
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountsGetAccount(email,false);
			if (account == null) {
				System.out.println("Account not found : " + email);
			} else {
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());
				System.out.println("-----------------");
				System.out.println("Updgrading");
				System.out.println("-----------------");
				dao.accountUpgradeAccount(email);
				account = dao.accountsGetAccount(email,false);
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());
				System.out.println("-----------------");
			}
		} else {
			System.out.println("Account email is zero length");
		}
	}
	
	private static void downgradeAccount(String email) {
		if (email.length() > 0) {
			System.out.println("-----------------");
			System.out.println("Looking up : " + email);
			System.out.println("-----------------");
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountsGetAccount(email,false);
			if (account == null) {
				System.out.println("Account not found : " + email);
			} else {
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());
				System.out.println("-----------------");
				System.out.println("Downgrading");
				System.out.println("-----------------");
				dao.accountDowngradeAccount(email);
				account = dao.accountsGetAccount(email,false);
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());
				System.out.println("-----------------");
			}
		} else {
			System.out.println("Account email is zero length");
		}
	}

	private static void domainStats() {
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Grabbing all Domains");
		System.out.println("-----------------");
		PaginatedScanList<Domain> domains = dao.getAllDomains();
		System.out.println("Number of Domains : " + domains.size());
		System.out.println("-----------------");
	}
	
	private static void accountStats() {
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Grabbing all Accounts");
		System.out.println("-----------------");
		PaginatedScanList<Account> accounts = dao.getAllAccounts();
		System.out.println("Number of Accounts : " + accounts.size());
		accounts = dao.getAllRedditAccounts();
		System.out.println("Number of Reddit Accounts : " + accounts.size());
		accounts = dao.getAllFacebookAccounts();
		System.out.println("Number of Facebook Accounts : " + accounts.size());
		System.out.println("-----------------");
		accounts = dao.getAllDonatedAccounts();
		System.out.println("Number Donated : " + accounts.size());
		accounts = dao.getAllFriendsAccounts();
		System.out.println("Number Friends : " + accounts.size());
		System.out.println("-----------------");
	}
	
	private static void backupDomains(String location) {
		File output = new File(location);
		if (output.exists()) {
			System.out.println("File Already Exists : " + output.getAbsolutePath());
			return;
		}
		
		File mainDir = output.getParentFile();
		if (mainDir.isFile()) {
			System.out.println("Parent is a file Error! : " + mainDir.getAbsolutePath());
			return;
		}
		
		if (!mainDir.exists()) {
			if (!mainDir.mkdirs()) {
				System.out.println("Parent Dir cannot be created : " + mainDir.getAbsolutePath());
				return;
			}
		}
		
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Backing up All Domains to : " + output.getAbsolutePath());
		System.out.println("-----------------");
		
		PaginatedScanList<Domain> domains = dao.getAllDomains();
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(output));
			
			bw.append("domainName,accountToken,currentIp,lastUpdated\n");
			for (Domain domain : domains) {
				bw.append(domain.getDomainName());
				bw.append(",");
				bw.append(domain.getAccountToken());
				bw.append(",");
				if (domain.getCurrentIp() == null || domain.getCurrentIp().length() == 0) {
					bw.append(" ");
				} else {
					bw.append(domain.getCurrentIp());
				}
				bw.append(",");
				if (domain.getCurrentIpV6() == null || domain.getCurrentIpV6().length() == 0) {
					bw.append(" ");
				} else {
					bw.append(domain.getCurrentIpV6());
				}
				bw.append(",");
				bw.append(domain.getLastUpdated());
				bw.append("\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void backupAccounts(String location) {
		
		File output = new File(location);
		if (output.exists()) {
			System.out.println("File Already Exists : " + output.getAbsolutePath());
			return;
		}
		
		File mainDir = output.getParentFile();
		if (mainDir.isFile()) {
			System.out.println("Parent is a file Error! : " + mainDir.getAbsolutePath());
			return;
		}
		
		if (!mainDir.exists()) {
			if (!mainDir.mkdirs()) {
				System.out.println("Parent Dir cannot be created : " + mainDir.getAbsolutePath());
			}
			return;
		}
		
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Backing up All Accounts to : " + output.getAbsolutePath());
		System.out.println("-----------------");
		
		PaginatedScanList<Account> accounts = dao.getAllAccounts();
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(output));
			
			bw.append("email,accountToken,lastUpdated,createdDate,accountType\n");
			for (Account account : accounts) {
				bw.append(account.getEmail());
				bw.append(",");
				bw.append(account.getAccountToken());
				bw.append(",");
				bw.append(account.getLastUpdated());
				bw.append(",");
				bw.append(account.getCreatedDate());
				bw.append(",");
				bw.append(account.getAccountType());
				bw.append("\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private static void restoreAccounts(String location) {
		
		System.out.println("Are you sure you want to do that Dave?");
		if (1 == 1) return;
		
		File input = new File(location);
		if (!input.exists()) {
			System.out.println("File Missing : " + input.getAbsolutePath());
			return;
		}
		
		if (!input.canRead()) {
			System.out.println("Cannot read file : " + input.getAbsolutePath());
			return;
		}
		
		System.out.println("-----------------");
		System.out.println("Restoring All Accounts into accountsv2 from : " + input.getAbsolutePath());
		System.out.println("-----------------");
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(input));
			
			String currentLine = br.readLine();
			
			if (currentLine == null || !currentLine.equals("email,accountToken,lastUpdated,createdDate,accountType")) {
				System.out.println("Header does not match : expected 'email,accountToken,lastUpdated,createdDate,accountType' got '"+currentLine+"'");
				return;
			}
			
			currentLine = br.readLine();
			
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			int count = 0;
			while (currentLine != null) {
				StringTokenizer st = new StringTokenizer(currentLine,",");
				if (st.countTokens() == 5) {
					dao.accountsCreateAccount(st.nextToken(),st.nextToken(),st.nextToken(),st.nextToken(),st.nextToken());
					count ++;
				} else {
					System.out.println("too few tokens ("+st.countTokens()+") in line : " + currentLine);
					return;
				}
				if (count % 100 == 0) {
					System.out.println("Restored : " + count);
				}
				// NEXT!
				currentLine = br.readLine();
			}
			System.out.println("Restored : " + count);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void restoreDomains(String location) {
		
		System.out.println("Are you sure you want to do that Dave?");
		if (1 == 1) return;
		
		File input = new File(location);
		if (!input.exists()) {
			System.out.println("File Missing : " + input.getAbsolutePath());
			return;
		}
		
		if (!input.canRead()) {
			System.out.println("Cannot read file : " + input.getAbsolutePath());
			return;
		}
		
		System.out.println("-----------------");
		System.out.println("Restoring up All Domains into domainsv2 from : " + input.getAbsolutePath());
		System.out.println("-----------------");
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(input));
			
			String currentLine = br.readLine();
			
			if (currentLine == null || !currentLine.equals("domainName,accountToken,currentIp,lastUpdated")) {
				System.out.println("Header does not match : expected 'domainName,accountToken,currentIp,lastUpdated' got '"+currentLine+"'");
				return;
			}
			
			currentLine = br.readLine();
			
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			int count = 0;
			while (currentLine != null) {
				count ++;
				StringTokenizer st = new StringTokenizer(currentLine,",");
				if (st.countTokens() == 5) {
					dao.domainsCreateDomain(st.nextToken(), st.nextToken(), st.nextToken().trim(), st.nextToken().trim(), st.nextToken());
					//System.out.println(st.nextToken() + "," + st.nextToken() + "," + st.nextToken() + "," + st.nextToken());
				} else {
					System.out.println("too few tokens ("+st.countTokens()+") in line : " + currentLine);
					return;
				}
				if (count % 100 == 0) {
					System.out.println("Restored : " + count);
				}
				// NEXT!
				currentLine = br.readLine();
			}
			System.out.println("Restored : " + count);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void clearDomains() {
		
		System.out.println("Are you sure you want to do that Dave?");
		if (1 == 1) return;
		
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Clearing all domains in domainsv2");
		System.out.println("-----------------");
		
		PaginatedScanList<Domain> domains = dao.getAllDomains();
		
		int count = 0;
		for (Domain domain : domains) {
			count++;
			dao.domainDeleteDomain(domain.getDomainName(), domain.getAccountToken());
			if (count % 100 == 0) {
				System.out.println("Cleared : " + count);
			}
		}
		System.out.println("Cleared : " + count);
		
	}
	
	private static void clearAccounts() {
		
		System.out.println("Are you sure you want to do that Dave?");
		if (1 == 1) return;
		
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		System.out.println("-----------------");
		System.out.println("Clearing all accounts in accountsv2");
		System.out.println("-----------------");
		
		PaginatedScanList<Account> accounts = dao.getAllAccounts();
		
		int count = 0;
		for (Account account : accounts) {
			count++;
			dao.accountDeleteAccountLeaveDomains(account.getEmail());
			if (count % 100 == 0) {
				System.out.println("Cleared : " + count);
			}
		}
		System.out.println("Cleared : " + count);
		
	}
	
	private static void findUser(String domainName) {
		if (domainName.length() > 0) {
			System.out.println("-----------------");
			System.out.println("Looking up : " + domainName);
			System.out.println("-----------------");
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountFindAccountByDomain(domainName);
			if (account == null) {
				System.out.println("Domain / Account not found : " + domainName);
			} else {
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());

				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				System.out.println("---------DOMAINS--------");
				for (Domain domain : domains) {
					System.out.println("Domain Name : " + domain.getDomainName());
					System.out.println("Domain IP : " + domain.getCurrentIp());
					System.out.println("Domain IPV6 : " + domain.getCurrentIpV6());
					System.out.println("Domain Last Updated : " + domain.getLastUpdated());
					System.out.println("Domain Locked : " + domain.getLocked());
					System.out.println("-----------------");
				}
			}
		} else {
			System.out.println("Domain name is zero length");
		}
	}
	
	private static void lockAccount(String domainName) {
		if (domainName.length() > 0) {
			System.out.println("-----------------");
			System.out.println("Looking up to Lock : " + domainName);
			System.out.println("-----------------");
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountFindAccountByDomain(domainName);
			if (account == null) {
				System.out.println("Domain / Account not found : " + domainName);
			} else {
				// LOCK ALL THE THINGS
				dao.lockAccount(account.getEmail());
				// RELOAD
				account = dao.accountFindAccountByDomain(domainName);
				// NOW DISPLAY
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());

				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				System.out.println("---------DOMAINS--------");
				for (Domain domain : domains) {
					System.out.println("Domain Name : " + domain.getDomainName());
					System.out.println("Domain IP : " + domain.getCurrentIp());
					System.out.println("Domain IPV6 : " + domain.getCurrentIpV6());
					System.out.println("Domain Last Updated : " + domain.getLastUpdated());
					System.out.println("Domain Locked : " + domain.getLocked());
					System.out.println("-----------------");
					s1.send(new SimpleOneLineMessage(domain.getDomainName()));
					s2.send(new SimpleOneLineMessage(domain.getDomainName()));
					s3.send(new SimpleOneLineMessage(domain.getDomainName()));
				}
				try {
					System.out.println("Sleeping for 5 seconds to let the domains flush");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Done");
			}
		} else {
			System.out.println("Domain name is zero length");
		}
	}
	
	private static void unlockAccount(String domainName) {
		if (domainName.length() > 0) {
			System.out.println("-----------------");
			System.out.println("Looking up to Unlock : " + domainName);
			System.out.println("-----------------");
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountFindAccountByDomain(domainName);
			if (account == null) {
				System.out.println("Domain / Account not found : " + domainName);
			} else {
				// LOCK ALL THE THINGS
				dao.unlockAccount(account.getEmail());
				// RELOAD
				account = dao.accountFindAccountByDomain(domainName);
				// NOW DISPLAY
				System.out.println("Account Token : " + account.getAccountToken());
				System.out.println("Account Type : " + account.getAccountType());
				System.out.println("Account Created Date : " + account.getCreatedDate());
				System.out.println("Account Email : " + account.getEmail());
				System.out.println("Account Last Updated : " + account.getLastUpdated());

				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				System.out.println("---------DOMAINS--------");
				for (Domain domain : domains) {
					System.out.println("Domain Name : " + domain.getDomainName());
					System.out.println("Domain IP : " + domain.getCurrentIp());
					System.out.println("Domain IPV6 : " + domain.getCurrentIpV6());
					System.out.println("Domain Last Updated : " + domain.getLastUpdated());
					System.out.println("Domain Locked : " + domain.getLocked());
					System.out.println("-----------------");
					s1.send(new SimpleOneLineMessage(domain.getDomainName()));
					s2.send(new SimpleOneLineMessage(domain.getDomainName()));
					s3.send(new SimpleOneLineMessage(domain.getDomainName()));
				}
				try {
					System.out.println("Sleeping for 5 seconds to let the domains flush");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Done");
			}
		} else {
			System.out.println("Domain name is zero length");
		}
	}
	
	private static void updateApp() {
		
		System.out.println("Downloading file (duckdns-dist) duckdns.war  --->  /var/tmp/duckdns.war");
		ClientConfiguration clientConfig = new ClientConfiguration();
		clientConfig.setProtocol(Protocol.HTTP);

		AmazonS3 conn = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider(), clientConfig);
		
		conn.getObject(new GetObjectRequest("duckdns-dist", "duckdns.war"),new File("/var/tmp/duckdns.war"));
		System.out.println("Done");
	}
	
}