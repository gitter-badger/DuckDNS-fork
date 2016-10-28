<?xml version="1.0" encoding="UTF-8"?>
<%@ page contentType="text/html; charset=UTF-8" import="
java.util.GregorianCalendar,
java.util.Calendar,
java.util.UUID,
org.duckdns.dao.model.Account,
org.duckdns.util.ServletUtils,
org.duckdns.util.FormatHelper,
org.duckdns.util.EnvironmentUtils,
org.duckdns.util.SessionHelper" %>
<%@ taglib prefix="module" tagdir="/WEB-INF/tags" %>
<%
SessionHelper.restoreSessionIfNeeded(request);

boolean isLoggedIn = false;
boolean isLoggedInPersona = false;
String sessionEmail = "";
Account account = null;

if (request.getSession() != null) {
	sessionEmail = (String) request.getSession().getAttribute("email");
	if (sessionEmail != null && sessionEmail.length() > 0) {
		account = (Account) request.getSession().getAttribute("account");
		if (account != null) {
			isLoggedIn = true;
		}
	}
}

String accountIcon = "ducky_icon";
if (isLoggedIn && account.getAccountType().equals(Account.ACCOUNT_DONATE)) {
	accountIcon = "ducky_icon_gold";
} else if (isLoggedIn && account != null && account.getAccountType().equals(Account.ACCOUNT_FRIENDS_OF_DUCK)) {
	accountIcon = "ducky_icon_diamond";
} else if (isLoggedIn && account != null && account.getAccountType().equals(Account.ACCOUNT_SUPER_DUCK)) {
	accountIcon = "ducky_icon_super";
} else if (isLoggedIn && account != null && account.getAccountType().equals(Account.ACCOUNT_NAUGHTY_DUCK)) {
	accountIcon = "ducky_icon_naughty";
}

String isProductionStr = "false";
if (EnvironmentUtils.isProduction()) {
	isProductionStr = "true";
}

String isLoggedInStr = "false";
String isLoggedInPersonaStr = "false";
String personaEmail = "";
if (isLoggedIn) {
	isLoggedInStr = "true";
	if (sessionEmail.endsWith("#persona")) {
		isLoggedInPersona = true;
		isLoggedInPersonaStr = "true";
		personaEmail = sessionEmail.substring(0,sessionEmail.length()-8);
	}
	pageContext.setAttribute("personaEmail", personaEmail);
	pageContext.setAttribute("sessionEmailStr", sessionEmail);
	pageContext.setAttribute("accountType", account.getAccountType());
	pageContext.setAttribute("accountToken", account.getAccountToken());
	pageContext.setAttribute("tokenGenerated", FormatHelper.convertShortDateToHumanReadableTimeAgo(account.getLastUpdated()));
	pageContext.setAttribute("createdDate", FormatHelper.toReadableDate(account.getCreatedDate()));
}

pageContext.setAttribute("isLoggedInStr", isLoggedInStr);
pageContext.setAttribute("isLoggedInPersonaStr", isLoggedInPersonaStr);
pageContext.setAttribute("accountIcon", accountIcon);
pageContext.setAttribute("isProductionStr", isProductionStr);
pageContext.setAttribute("state", UUID.randomUUID().toString().replaceAll("-", ""));
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
	<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
	<!--[if gt IE 8]><!--> <html class="no-js"> <!--<![endif]-->
	<head>
		<meta charset="utf-8" />
		<title>Duck DNS - spec</title>
		<meta name="viewport" content="initial-scale=1.0" />
		<meta name="description" content="api specification" />
		<meta name="keywords" content="spec, api, info, manual" />
		<meta name="author" content="" />
		
		<meta http-equiv="X-UA-Compatible" content="IE=Edge" />
		<module:includes/>
<c:if test="${isLoggedInStr eq 'false' or isLoggedInPersonaStr eq 'true'}">
		<script src="https://login.persona.org/include.js"></script>
</c:if>
	</head>
	
	<body>
		<header id="ee-masthead" class="ee-masthead" role="banner">
			<module:header 
				section="spec" 
				sessionEmail="${sessionEmailStr}"
				isLoggedIn="${isLoggedInStr}"
				isLoggedInPersonaStr="${isLoggedInPersonaStr}"
				state="${state}"
			/>
		</header>
		<main id="main" tabindex="0" role="main" class="ducky">
		    <section class="module panels">
		    	<module:accountinfo 
					section="why"
					sessionEmail="${sessionEmailStr}"
					isLoggedIn="${isLoggedInStr}"
					accountIcon="${accountIcon}"
					accountType="${accountType}"
					accountToken="${accountToken}"
					tokenGenerated="${tokenGenerated}"
					createdDate="${createdDate}"
				/>
		        <div class="container">
		        	<div class="one-true white">
		                <div class="col span-12">
		                    <div class="panel white">
						  		<div class="hero-unit">
									<h3>HTTP API Specification</h3>
									<p>The update URL can be requested on HTTPS or HTTP.  Is it recommended that you <b>always use HTTPS</b><br/>
									We provide HTTP services for unfortunate users that have HTTPS blocked<br/>
									</p>
									
									<p>
									You can update your domain(s) with a single HTTPS get to DuckDNS
									</p>
<pre>https://www.duckdns.org/update?domains={YOURVALUE}&amp;token={YOURVALUE}[&amp;ip={YOURVALUE}][&amp;ipv6={YOURVALUE}][&amp;verbose=true][&amp;clear=true]</pre>
									<p>The domain can be a single domain - or a comma separated list of domains.<br/>
									The domain does not need to include the .duckdns.org part of your domain, just the subname.<br/>
									If you do not specify the IP address, then it will be detected - this only works for IPv4 addresses<br/>
									You can put either an IPv4 or an IPv6 address in the <b>ip parameter</b><br/>
									If you want to update BOTH of your IPv4 and IPv6 records at once, then you can use the optional parameter <b>ipv6</b><br/>
									to clear both your records use the optional parameter <b>clear=true</b><br/>
									</p>
									
									<p>A normal <b>good response</b> is</p>
<pre>OK</pre>
									<p>A normal <b>bad response</b> is</p>
<pre>KO</pre>
									<p>if you add the <b>&amp;verbose=true</b> parameter to your request, then OK responses have more information</p>
<pre>OK 
127.0.0.2 [The current IP address for your update - can be blank]
2002:DB7::21f:5bff:febf:ce22:8a2e [The current IPV6 address for your update - can be blank]
UPDATED [UPDATED or NOCHANGE]</pre>
									<p><b>HTTP Parameters</b><br/>
									<b>domains</b> - REQUIRED - comma separated list of the subnames you want to update<br/>
									<b>token</b> - REQUIRED - your account token<br/>
									<b>ip</b> - OPTIONAL - if left blank we detect IPv4 addresses, if you want you can supply a valid IPv4 or IPv6 address<br/>
									<b>ipv6</b> - OPTIONAL - a valid IPv6 address, if you specify this then the autodetection for ip is not used<br/>
									<b>verbose</b> - OPTIONAL - if set to true, you get information back about how the request went<br/>
									<b>clear</b> - OPTIONAL - if set to true, the update will ignore all ip's and clear both your records
									</p>
									
									<br/><br/><br/>
									<h3>Special no-parameter request format</h3>
									<p>
									Some very basic routers can only make requests without parameters<br/>
									For these requirements the following request is possible
<pre>https://duckdns.org/update/{YOURDOMAIN}/{YOURTOKEN}[/{YOURIPADDRESS}]</pre>
									<p><b>Restrictions</b><br/>
									<b>YOURDOMAIN</b> - REQUIRED - only a single subdomain<br/>
									<b>YOURTOKEN</b> - REQUIRED - your account token<br/>
									<b>YOURIPADDRESS</b> - OPTIONAL - if left blank we detect IPv4 addresses, if you want to over-ride this, with a valid IPv4 or IPv6 address
									</p>
									<br/>
						  		</div>
							</div>
		                </div>
		            </div>
		    	</div>
		    </section>
		</main>
		<footer>
			<module:footer
				sessionEmail="${sessionEmailStr}"
				isLoggedIn="${isLoggedInStr}"
			/>
		</footer>
		<script>
			document.write('<scr'+'ipt src="'+document.location.protocol+'//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></scr'+'ipt>');
		</script>
		<script src="js/ducky-8.js"></script>
<c:if test="${isProductionStr eq 'true' }">
		<script src="js/tracking.js"></script>
</c:if>
<c:if test="${isLoggedInStr eq 'false' or isLoggedInPersonaStr eq 'true'}">
	<module:personajs personaEmail="${personaEmail}"/>
</c:if>
	</body>
</html>