package org.duckdns.servlets;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.duckdns.comms.Sender;
import org.duckdns.comms.clients.BasicSender;
import org.duckdns.comms.messages.SimpleOneLineMessage;
import org.duckdns.dao.AmazonDynamoDBDAO;
import org.duckdns.dao.model.Account;
import org.duckdns.dao.model.Domain;
import org.duckdns.util.DomainNameFilterer;
import org.duckdns.util.GoogleAnalyticsHelper;
import org.duckdns.util.SessionHelper;
import org.duckdns.util.EnvironmentUtils;
import org.duckdns.util.SecurityHelper;
import org.duckdns.util.ServletUtils;
import org.duckdns.util.ValidationUtils;

public class DomainsServlet extends javax.servlet.http.HttpServlet {

	private static final long serialVersionUID = -1;
	private static final Log LOG = LogFactory.getLog(DomainsServlet.class);

	private ServletContext context;
	private Sender s1;
	private Sender s2;
	private Sender s3;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		context = config.getServletContext();
		LOG.debug("context: " + context);
		s1 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS1_INTERNAL(), 10025, 3, 20, 100, new BasicSender());
		s2 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS2_INTERNAL(), 10025, 3, 20, 100, new BasicSender());
		s3 = new Sender(EnvironmentUtils.getInstance().getFIXED_IP_NS3_INTERNAL(), 10025, 3, 20, 100, new BasicSender());
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			SessionHelper.restoreSessionIfNeeded(req);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (SecurityHelper.hasValidSession(req)) {
			String addDomain = req.getParameter("addDomain");
			String deleteDomain = req.getParameter("deleteDomain");
			String updateIp = req.getParameter("updateIp");
			String updateIpV6 = req.getParameter("updateIpV6");
			if (addDomain != null) {
				addDomain(req, addDomain);
			} else if (deleteDomain != null) {
				deleteDomain(req, deleteDomain);
			} else if (updateIp != null) {
				updateIp(req, updateIp);
			} else if (updateIpV6 != null) {
				updateIpV6(req, updateIpV6);
			}
		}
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}


	public static void reloadDomains(HttpServletRequest req) throws IOException {
		Account account = (Account) req.getSession().getAttribute("account");
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
		req.getSession().setAttribute("domains", domains);
	}
	
	private void updateIpV6(HttpServletRequest req, String updateIpV6) throws IOException {
		String domainName = req.getParameter("domainName");
		if (domainName == null) {
			// HACKED REQUEST - NO FEEDBACK
			return;
		}
		
		// DOES NOT CHECK OWNERSHIP JUST VALIDITY
		if (ValidationUtils.isValidSubDomain(domainName)) {
		
			Account account = (Account) req.getSession().getAttribute("account");
			// NAUGHTY DUCK
			if (account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
				ServletUtils.setFeedback(req, false,"unable to update IP because the account is locked");
				return;
			}
			
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			
			// IF THE IP IS BLANK USE THEIR CURRENT ONE
			if (updateIpV6.length() == 0) {
				// updateIp = ServletUtils.getAddressFromRequest(req);
				// BLANK ALLOWED NOW!
			}
			
			// THE DAO METHOD CHECKS OWNERSHIP
			int result = dao.domainUpdateIpV6(updateIpV6, domainName, account.getAccountToken());
			if (result != AmazonDynamoDBDAO.UPDATE_RETURN_FAILED) {
				
				if (result == AmazonDynamoDBDAO.UPDATE_RETURN_UPDATED) {
					// COOL expire the DNS CACHE(s)
					s1.send(new SimpleOneLineMessage(domainName));
					s2.send(new SimpleOneLineMessage(domainName));
					s3.send(new SimpleOneLineMessage(domainName));
					// IF IT UPDATED THEN UPDATE THE SESSION
					List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
					req.getSession().setAttribute("domains", domains);
					ServletUtils.setFeedback(req, true,"ipv6 address for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong> updated to <strong>" + updateIpV6 + "</strong>");
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_UPDATE,"","");
				} else {
					ServletUtils.setFeedback(req, false,"ipv6 address for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong> was already <strong>" + updateIpV6 + "</strong> not updated");
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_UPDATE_SAME,"","");
				}
				
			} else {
				// IP ADRESS MUST BE BADLY FORMED
				ServletUtils.setFeedback(req, false,"invalid ipv6 address entered for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong>");
			}
		}
	}

	private void updateIp(HttpServletRequest req, String updateIp) throws IOException {
		
		String domainName = req.getParameter("domainName");
		if (domainName == null) {
			// HACKED REQUEST - NO FEEDBACK
			return;
		}
		
		// DOES NOT CHECK OWNERSHIP JUST VALIDITY
		if (ValidationUtils.isValidSubDomain(domainName)) {
		
			Account account = (Account) req.getSession().getAttribute("account");
			// NAUGHTY DUCK
			if (account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
				ServletUtils.setFeedback(req, false,"unable to update IP because the account is locked");
				return;
			}
			
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			
			// IF THE IP IS BLANK USE THEIR CURRENT ONE
			if (updateIp.length() == 0) {
				// updateIp = ServletUtils.getAddressFromRequest(req);
				// BLANK ALLOWED NOW!
			}
			
			// THE DAO METHOD CHECKS OWNERSHIP
			int result = dao.domainUpdateIp(updateIp, domainName, account.getAccountToken());
			if (result != AmazonDynamoDBDAO.UPDATE_RETURN_FAILED) {
				
				if (result == AmazonDynamoDBDAO.UPDATE_RETURN_UPDATED) {
					// COOL expire the DNS CACHE(s)
					s1.send(new SimpleOneLineMessage(domainName));
					s2.send(new SimpleOneLineMessage(domainName));
					s3.send(new SimpleOneLineMessage(domainName));
					// IF IT UPDATED THEN UPDATE THE SESSION
					List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
					req.getSession().setAttribute("domains", domains);
					ServletUtils.setFeedback(req, true,"ip address for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong> updated to <strong>" + updateIp + "</strong>");
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_UPDATE,"","");
				} else {
					ServletUtils.setFeedback(req, false,"ip address for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong> was already <strong>" + updateIp + "</strong> not updated");
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_UPDATE_SAME,"","");
				}
				
			} else {
				// IP ADRESS MUST BE BADLY FORMED
				ServletUtils.setFeedback(req, false,"invalid ip address entered for <strong>" + domainName + EnvironmentUtils.OUR_DOMAIN + "</strong>");
			}
		}
	}

	private void deleteDomain(HttpServletRequest req, String deleteDomain) throws IOException {
		Account account = (Account) req.getSession().getAttribute("account");
		// NAUGHTY DUCK
		if (account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
			ServletUtils.setFeedback(req, false,"unable to delete Domain because the account is locked");
			return;
		}
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		
		// CHECK DOMAIN IS VALID
		if (ValidationUtils.isValidSubDomain(deleteDomain)) {
			
			if (dao.domainDeleteDomain(deleteDomain, account.getAccountToken())) {
				// COOL expire the DNS CACHE(s)
				s1.send(new SimpleOneLineMessage(deleteDomain));
				s2.send(new SimpleOneLineMessage(deleteDomain));
				s3.send(new SimpleOneLineMessage(deleteDomain));
				// IF IT DELETED THEN UPDATE THE SESSION
				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				req.getSession().setAttribute("domains", domains);
				ServletUtils.setFeedback(req, true,"domain <strong>" + deleteDomain + EnvironmentUtils.OUR_DOMAIN +"</strong> deleted");
				GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_DELETE_DOMAIN,"","");
			}
		}
	}

	private void addDomain(HttpServletRequest req, String addDomain) throws IOException {
		// add domain by currentToken
		Account account = (Account) req.getSession().getAttribute("account");
		// NAUGHTY DUCK
		if (account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
			ServletUtils.setFeedback(req, false,"unable to add Domain because the account is locked");
			return;
		}
		AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
		addDomain = addDomain.toLowerCase();
		// CHECK DOMAIN IS VALID
		if (ValidationUtils.isValidSubDomain(addDomain)) {
			
			// TEST THE DOMAIN IS NOT STUPID LONG
			if (addDomain.length() > Domain.MAX_DOMAIN_LENGTH) {
				ServletUtils.setFeedback(req, false,"domain entered is too long ("+addDomain.length()+")");
				return;
			}
			
			// TEST THE DOMAIN IS NOT IN BANNED LIST
			if (!DomainNameFilterer.getInstance().isAllowed(addDomain)) {
				// EVEN IF BANNED GIVE SAME MESSAGE - HA!
				ServletUtils.setFeedback(req, false,"sorry the domain <strong>" + addDomain + EnvironmentUtils.OUR_DOMAIN + "</strong> is already taken by another user");
				return;
			}
			
			// CHEAP TEST IF THEY ALREADY HAVE MAX IN SESSION THEN DIE
			List<Domain> sessionDomains = (List<Domain>) req.getSession().getAttribute("domains");
			int maxDomains = account.getMaxDomains();
			
			if (sessionDomains.size() < maxDomains) {
				// OK CHEAP TEST PASSED - LETS DO THE EXPENSIVE ONE
				if (dao.domainsGetDomainsByToken(account.getAccountToken()).size() < maxDomains) {
			
					// CHECK DOMAIN IS AVAILIABLE
					Domain testDomain = dao.domainGetDomain(addDomain);
					if (testDomain == null) {
						// SET THE DOMAIN TO THE CURRENT IP
						String currentIp = ServletUtils.getAddressFromRequest(req);
						Domain domain = dao.domainsCreateDomain(addDomain, account.getAccountToken(), currentIp);
						if (domain != null) {
							List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
							req.getSession().setAttribute("domains", domains);
							ServletUtils.setFeedback(req, true,"domain <strong>" + addDomain + EnvironmentUtils.OUR_DOMAIN + "</strong> added to your account");
							// Flush any Negative Caching on DNS
							s1.send(new SimpleOneLineMessage(addDomain));
							s2.send(new SimpleOneLineMessage(addDomain));
							s3.send(new SimpleOneLineMessage(addDomain));
							GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_DOMAIN,GoogleAnalyticsHelper.ACTION_CREATE_DOMAIN,"","");
						}
					} else {
						ServletUtils.setFeedback(req, false,"sorry the domain <strong>" + addDomain + EnvironmentUtils.OUR_DOMAIN + "</strong> is already taken by another user");
					}
				}
			} else {
				ServletUtils.setFeedback(req, false,"maximum domains reached ("+maxDomains+") : unable to add <strong>" + addDomain + EnvironmentUtils.OUR_DOMAIN + "</strong>");
			}
		} else {
			ServletUtils.setFeedback(req, false,"invalid domain entered, valid characters are : A-Z, 0-9, -");
		}
		
	}
}
