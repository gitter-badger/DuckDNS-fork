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
import org.apache.http.client.ClientProtocolException;
import org.duckdns.dao.AmazonDynamoDBDAO;
import org.duckdns.dao.model.Account;
import org.duckdns.dao.model.Domain;
import org.duckdns.facebook.FacebookOAuth;
import org.duckdns.persona.PersonaAuth;
import org.duckdns.reddit.RedditOAuth;
import org.duckdns.twitter.TwitterDetails;
import org.duckdns.twitter.TwitterOAuth;
import org.duckdns.util.EnvironmentUtils;
import org.duckdns.util.GoogleAnalyticsHelper;
import org.duckdns.util.SecurityHelper;
import org.duckdns.util.ServletUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Google2Api;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.plus.samples.verifytoken.Checker;


public class LoginServlet extends javax.servlet.http.HttpServlet {

	private static final long serialVersionUID = -1;
	private static final Log LOG = LogFactory.getLog(LoginServlet.class);

	private ServletContext context;
	private OAuthService twitterOAuthservice;
	private OAuthService googleOAuthservice;
	
	/**
	 * {@inheritDoc}
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		context = config.getServletContext();

		LOG.debug("context: " + context);
		
		twitterOAuthservice = new ServiceBuilder()
			.provider(TwitterApi.SSL.class)
			.apiKey(EnvironmentUtils.getInstance().getTwitterApiKey())
			.apiSecret(EnvironmentUtils.getInstance().getTwitterApiSecret())
			.callback("https://www.duckdns.org/login")
			.build();
		
		googleOAuthservice = new ServiceBuilder()
			.provider(Google2Api.class)
			.apiKey(EnvironmentUtils.getInstance().getGoogleApiKey())
			.apiSecret(EnvironmentUtils.getInstance().getGoogleApiSecret())
			.callback("https://www.duckdns.org/login-google")
			.scope("email")
			.build();
		
		// KICK OFF THE DB CLEANER
		( new Thread("DuckDNS DB Cleaner") {
			public void run() {
				while (true) {
					try {
						AmazonDynamoDBDAO.getInstance().sessionsCleanUpOldSessions();
					} catch (Throwable tw) {}
					try {
						Thread.sleep(60*1000);
					} catch (InterruptedException e) {}
				}
			}
		} ).start();
		
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		if (req.getParameter("generateRequest") != null) {
			generateARequestToken(req, resp);
			return;
		} else {
			processReturnFromAOuth(req, resp);
			return;
		}
	}
	
	private void generateARequestToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if ("twitter".equals(req.getParameter("generateRequest"))) {
			Token requestToken = twitterOAuthservice.getRequestToken();
			req.getSession().setAttribute("requestToken",requestToken);
			String redirectURL = twitterOAuthservice.getAuthorizationUrl(requestToken);
			resp.sendRedirect(redirectURL);
			return;
		} else if ("google".equals(req.getParameter("generateRequest"))) {
			String redirectURL = googleOAuthservice.getAuthorizationUrl(null);
			resp.sendRedirect(redirectURL);
			return;
		}
	}

	private void processReturnFromAOuth (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// REDDIT & GOOGLE
		String code = req.getParameter("code");
		// FACEBOOK
		String access_token = req.getParameter("access_token");
		// TWITTER
		String oauth_token = req.getParameter("oauth_token");
		String oauth_verifier = req.getParameter("oauth_verifier");
		// PATH
		String path = req.getServletPath();
		if (code != null) {
			if (path != null && path.equals("/login-google")) {
				// GOOGLE LOGIN
				this.processGoogleReturn(code, req, resp);
				return;
			} else if (path != null && path.equals("/login-persona")) {
				// PERSONA LOGIN
				this.processPersonaReturn(code, req, resp);
				return;
			} else {
				// REDDIT LOGIN
				this.processRedditReturn(code, req, resp);
				return;
			}
		} else if(oauth_verifier != null) {
			// TWITTER LOGIN
			this.processTwitterReturn(oauth_token, oauth_verifier, req, resp);
			return;
		} else if(access_token != null) {
			// FACEBOOK LOGIN
			this.processFacebookReturn(access_token, req, resp);
			return;
		} else {
			this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
			return;
		}
	}
	
	private void processPersonaReturn(String assertion, HttpServletRequest req, HttpServletResponse resp) throws ClientProtocolException, IOException {
		String personaUserName = PersonaAuth.getUserEmail(assertion,ServletUtils.getHostNameFromRequest(req),ServletUtils.isSecureFromRequest(req));
		if (personaUserName != null) {
			String personaName = personaUserName+"#persona";
			req.getSession().setAttribute("email", personaName);
			// LOAD FROM THE DYANAMO DB
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountsGetAccount(personaName);
			if (account == null) {
				ServletUtils.setFeedback(req, false,EnvironmentUtils.ACCOUNTS_LOCKED);
				GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId("locked"),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_READ_ONLY,"","");
			} else {
				req.getSession().setAttribute("account", account);
				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				req.getSession().setAttribute("domains", domains);
				// PLACE THE UPDATED ITEM INTO THE SESSION CACHE
				if (EnvironmentUtils.isDynamoSessionCache()) {
					AmazonDynamoDBDAO.getInstance().sessionsCreateSession(req.getSession().getId(), account.getEmail(), ServletUtils.getAddressFromRequest(req));
				}
				GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_LOGIN_PERSONA,"","");
			}
			resp.setStatus(200);
			return;
		}
		// WE GET HERE ITS BAD
		resp.sendError(500);
	}

	private void processTwitterReturn(String oauth_token, String oauth_verifier, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		Verifier verifier = new Verifier(oauth_verifier);
		Object o = req.getSession().getAttribute("requestToken");
		if (o instanceof Token) {
			Token requestToken = (Token) o;
			Token accessToken = twitterOAuthservice.getAccessToken(requestToken, verifier);
			if (accessToken != null) {
				
				OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/account/verify_credentials.json");
				twitterOAuthservice.signRequest(accessToken, request);
			    Response response = request.send();
			    
			    // CLEAR THE TOKEN
			    req.getSession().removeAttribute("requestToken");
				
			    TwitterDetails twitterDetails = TwitterOAuth.getUserDetails(response.getBody());
				if (twitterDetails != null) {
					
					String twitterName = twitterDetails.getTwitterId()+"@twitter";
					req.getSession().setAttribute("email", twitterName);
					// LOAD FROM THE DYANAMO DB
					AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
					Account account = dao.accountsGetAccount(twitterName);
					if (account == null) {
						ServletUtils.setFeedback(req, false,EnvironmentUtils.ACCOUNTS_LOCKED);
						GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId("locked"),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_READ_ONLY,"","");
					} else {
						req.getSession().setAttribute("account", account);
						List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
						req.getSession().setAttribute("domains", domains);
						// PLACE THE UPDATED ITEM INTO THE SESSION CACHE
						if (EnvironmentUtils.isDynamoSessionCache()) {
							AmazonDynamoDBDAO.getInstance().sessionsCreateSession(req.getSession().getId(), account.getEmail(), ServletUtils.getAddressFromRequest(req));
						}
						GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_LOGIN_TWITTER,"","");
					}
					this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
					return;
				}
			}
		}
		
		// WE GET HERE ITS BAD
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}
	
	private void processFacebookReturn(String access_token, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String facebookEmail = FacebookOAuth.getUserEmail(access_token);
		if (facebookEmail != null) {
			String redditName = facebookEmail+"#facebook";
			req.getSession().setAttribute("email", redditName);
			// LOAD FROM THE DYANAMO DB
			AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
			Account account = dao.accountsGetAccount(redditName);
			if (account == null) {
				ServletUtils.setFeedback(req, false,EnvironmentUtils.ACCOUNTS_LOCKED);
				GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId("locked"),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_READ_ONLY,"","");
			} else {
				req.getSession().setAttribute("account", account);
				List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
				req.getSession().setAttribute("domains", domains);
				// PLACE THE UPDATED ITEM INTO THE SESSION CACHE
				if (EnvironmentUtils.isDynamoSessionCache()) {
					AmazonDynamoDBDAO.getInstance().sessionsCreateSession(req.getSession().getId(), account.getEmail(), ServletUtils.getAddressFromRequest(req));
				}
				GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_LOGIN_FACEBOOK,"","");
			}
			this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
			return;
		}
		// WE GET HERE ITS BAD
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}
	
	private void processRedditReturn(String code, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String token = RedditOAuth.getAccessToken(code, EnvironmentUtils.getProtocol()+"://"+ServletUtils.getHostNameFromRequest(req)+"/login");
		if (token != null) {
			String redditUserName = RedditOAuth.getUserName(token);
			if (redditUserName != null) {
				String redditName = redditUserName+"@reddit";
				req.getSession().setAttribute("email", redditName);
				// LOAD FROM THE DYANAMO DB
				AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
				Account account = dao.accountsGetAccount(redditName);
				if (account == null) {
					ServletUtils.setFeedback(req, false,EnvironmentUtils.ACCOUNTS_LOCKED);
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId("locked"),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_READ_ONLY,"","");
				} else {
					req.getSession().setAttribute("account", account);
					List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
					req.getSession().setAttribute("domains", domains);
					// PLACE THE UPDATED ITEM INTO THE SESSION CACHE
					if (EnvironmentUtils.isDynamoSessionCache()) {
						AmazonDynamoDBDAO.getInstance().sessionsCreateSession(req.getSession().getId(), account.getEmail(), ServletUtils.getAddressFromRequest(req));
					}
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_LOGIN_REDDIT,"","");
				}
				this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
				return;
			}
		}
		// WE GET HERE ITS BAD
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}

	private void processGoogleReturn(String code, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		Verifier verifier = new Verifier(code);
		Token accessToken = googleOAuthservice.getAccessToken(null, verifier);

		if (accessToken != null) {
			// Now we decrypt and Verify the JWT
			Checker checker = new Checker(new String[]{EnvironmentUtils.getInstance().getGoogleApiKey()}, EnvironmentUtils.getInstance().getGoogleApiKey());
			GoogleIdToken.Payload jwt = checker.check(accessToken.getToken());
			
	        if (jwt != null) {
	        	
	        	String email = jwt.getEmail();
	        	req.getSession().setAttribute("email", email);
	        	AmazonDynamoDBDAO dao = AmazonDynamoDBDAO.getInstance();
				Account account = dao.accountsGetAccount(email);
				if (account == null) {
					ServletUtils.setFeedback(req, false,EnvironmentUtils.ACCOUNTS_LOCKED);
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId("locked"),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_READ_ONLY,"","");
				} else {
					req.getSession().setAttribute("account", account);
					List<Domain> domains = dao.domainsGetDomainsByToken(account.getAccountToken());
					req.getSession().setAttribute("domains", domains);
					// PLACE THE UPDATED ITEM INTO THE SESSION CACHE
					if (EnvironmentUtils.isDynamoSessionCache()) {
						AmazonDynamoDBDAO.getInstance().sessionsCreateSession(req.getSession().getId(), account.getEmail(), ServletUtils.getAddressFromRequest(req));
					}
					GoogleAnalyticsHelper.RecordAsyncEvent(SecurityHelper.generateEncryptedId(account.getEmail()),GoogleAnalyticsHelper.CATEGORY_USER,GoogleAnalyticsHelper.ACTION_LOGIN_GOOGLE,"","");
				}
				this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
				return;
	        }
		}
		
		// WE GET HERE ITS BAD
		this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
	}

}
