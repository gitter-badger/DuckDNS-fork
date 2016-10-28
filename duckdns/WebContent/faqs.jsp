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
		<title>Duck DNS - faqs</title>
		<meta name="viewport" content="initial-scale=1.0" />
		<meta name="description" content="frequently asked questions" />
		<meta name="keywords" content="why,because" />
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
				section="faqs" 
				sessionEmail="${sessionEmailStr}"
				isLoggedIn="${isLoggedInStr}"
				isLoggedInPersonaStr="${isLoggedInPersonaStr}"
				state="${state}"
			/>
		</header>
		<main id="main" tabindex="0" role="main" class="ducky">
		    <section class="module panels">
		    	<module:accountinfo 
					section="faqs"
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
									<p class="text-bold">Q: how is this free?</p>
									<p>A: we have created a service that we have wanted for ourselves and believe in keeping it free or as cheap as possible to cover running costs, our attempt is to host and run the service at almost zero outlay excluding our time.</p>
									<br/>
									<p class="text-bold">Q: what data do you keep?</p>
									<p>A: the simple answer is: as little as possible. we only store your email address, token, domains and of course your target ips, we don't keep any logs.</p>
									<br/>
									<p class="text-bold">Q: what happens to my data?</p>
									<p>A: this goes into a private database and will never be sold on, we have no plans to send any emails to this list</p>
									<br/>
									<p class="text-bold">Q: are you secure?</p>
									<p>A: our entire service is run over https with a valid 256bit signed ssl certificate, this is provided for Free by <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://www.startssl.com/">StartSSL</a></p>
									<br/>
									<p class="text-bold">Q: can I script my own update?</p>
									<p>A: yes you can do this on http or https.  you can comma separate the domains if you want to update more than one, the ip parameter is optional, if you leave it blank we detect your gateway ip
									<br/>https://www.duckdns.org/update?domains=ben&token=064a0540-864c-4f0f-8bf5-23857452b0c1&ip=<br/>
									<a style="color:#cccccc;text-decoration:underline;" href="spec.jsp">see the spec page</a> for all the details and options.</p>
									<br/>
									<p class="text-bold">Q: my router can only do basic https GETs, I cannot send parameters, do you support this?</p>
									<p>A: yes either on http or https, you can make a simple request with the following format, the ip parameter is optional, if you leave it blank we detect your gateway ip. see the <a style="color:#cccccc;text-decoration:underline;" href="install.jsp?tab=allied-telesis">allied telesis</a> install page.
									<br/>https://duckdns.org/update/exampledomain/yourtoken/ipaddress
									<br/>https://duckdns.org/update/exampledomain/yourtoken</p>
									<br/>
									<p class="text-bold">Q: why does my ip not reverse to a host when I type host xxx.xxx.xxx.xxx?</p>
									<p>A: we don't have the authority to update the reverse zones hosted by your ISP</p>
									<br/>
									<p class="text-bold">Q: why can't you detect IPv6 addresses?</p>
									<p>A: our service is hosted in AWS, they <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/elb-internet-facing-load-balancers.html">do not support IPv6 on Elastic Load Balancers</a> for the account type we have (VPC), when amazon do support it, we will start detecting it.</p>
									<br/>
									<p class="text-bold">Q: can I use this for my email?</p>
									<p>A: yes, but you are going to have to sort out installing a mail server etc yourself.  The MX record will be the same as your normal A record.</p>
									<br/>
									<p class="text-bold">Q: I want to change my token, how do I do this?</p>
									<p>A: on the Account page, in the header, next to your logged in email are three small lines, click on these, then click on the recreate token button</p>
									<br/>
									<p class="text-bold">Q: I want to use my own Domain name with DuckDNS, can I do this?</p>
									<p>A: Yes you can. At your NAME provider set your purchased record as a <b>CNAME</b> to your duckdns.org record.<br/>
									<b>www.ilikeweasels.org CNAME weasels.duckdns.org</b></p>
									<br/>
									<p class="text-bold">Q: Why do I have to specify my IPv6 address, can't you just detect it?</p>
									<p>A: Amazon Web Services (where we host our servers) do not support IPv6 addresses on servers hosted in VPC<br/>
									VPC is the new/best hosting in AWS, when <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/elb-internet-facing-load-balancers.html">AWS support IPv6 on VPC Elastic Load Balancers on VPC</a>, then we will start detecting it.</p>
									<br/>
									<p class="text-bold">Q: Can I view the source code?</p>
									<p>A: Yes, we are GNU GPLv3 licensed. The source code is available as a <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://drive.google.com/folderview?id=0B-jet_9PYdwyYXNzVnIyMVI4NG8&usp=sharing">zip file on Google Drive</a>.</p>
									<br/>
									<p class="text-bold">Q: Is your custom DNS Server responding properly</p>
									<p>A: we use a very handy online tool to check our DNS server meets standards <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://www.intodns.com/duckdns.org">intodns.com</a>, also we highly recommend the linux command dig</p>
									<br/>
									<p class="text-bold">Q: Why use DuckDNS there are many other DDNS providers that are Free</p>
									<p>A: We come recommended by <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://www.gnutomorrow.com/best-free-dynamic-dns-services-in-2013/">Gnu Tomorrow</a>, our service is as focused as possible, easy to setup, secure and free solution.</p>
									<br/>
									<p class="text-bold">Q: I want to completely delete my account?</p>
									<p>A: fair enough, on the Account page, in the header, next to your logged in email are three small lines, click on these, then click the delete account button</p>
									<br/>
									<p class="text-bold">Q: Do you have an Abuse policy?</p>
									<p>A: yes, we will block, as much as we can anyone who is abusing our service, if you have any issues please use the <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://plus.google.com/communities/111042707043677579973">Google+ Community</a> to contact us</p>
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