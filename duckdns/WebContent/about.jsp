<?xml version="1.0" encoding="UTF-8"?>
<%@ page contentType="text/html; charset=UTF-8" import="
java.util.GregorianCalendar,
java.util.Calendar,
java.util.UUID,
org.duckdns.dao.model.Account,
org.duckdns.util.ServletUtils,
org.duckdns.util.FormatHelper,
org.duckdns.util.EnvironmentUtils,org.duckdns.util.SessionHelper" %>
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
		<title>Duck DNS - about</title>
		<meta name="viewport" content="initial-scale=1.0" />
		<meta name="description" content="about duckdns" />
		<meta name="keywords" content="about,reasons" />
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
				section="about" 
				sessionEmail="${sessionEmailStr}"
				isLoggedIn="${isLoggedInStr}"
				isLoggedInPersonaStr="${isLoggedInPersonaStr}"
				state="${state}"
			/>
		</header>
		<main id="main" tabindex="0" role="main" class="ducky">
		    <section class="module panels">
		    	<module:accountinfo 
					section="about"
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
									<p class="text-bold">what does the service actually do?</p>
									<p>Duck DNS is a free service which will point a DNS (sub domains of duckdns.org) to an IP of your choice</p>
									<br/>
									<p class="text-bold">who are we?</p>
									<p>the team consists of two software engineers who each have worked in the industry for over 15 years</p>
									<br/>
									<p class="text-bold">why do I need a dynamic DNS service</p>
									<p>DDNS is a handy way for you to refer to a server/router with an easily rememberable name, where the servers ip address is likely to change</p>
									<p>when your router reconnects, or ec2 server reboots, its ip address is set by the provider of that connection, this means it may update at any time</p>
									<br/>
									<p class="text-bold">why make a free DDNS service?</p>
									<p>because we can, because before we started we couldn't, learning is fun</p>
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