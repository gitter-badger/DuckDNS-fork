<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:choose>
	<c:when test="${not empty dynattrs.tab and dynattrs.tab == 'mikrotik'}">
		<c:set var="divstyle" value=" active" />
	</c:when>
	<c:otherwise>
		<c:set var="divstyle" value="" />
	</c:otherwise>
</c:choose>

					<div class="tab-pane${divstyle}" id="mikrotik">
						<h2>mikrotik</h2>
						these instructions are for <a target="new" style="color:#cccccc;text-decoration:underline;" href="http://www.mikrotik.com/">mikrotik routers</a><br/>

<c:choose>
	<c:when test="${dynattrs.isLoggedIn == 'yes' and dynattrs.exampleSingleDomain == 'exampledomain'}">
					if you want the configuration for a domain you have, <strong>use the drop down box</strong> above to select the domain<br/>	
	</c:when>
	<c:when test="${dynattrs.isLoggedIn == 'yes' and dynattrs.exampleSingleDomain != 'exampledomain'}">
					The example below is for the domain <strong>${dynattrs.exampleSingleDomain}</strong><br/>
					if you want the configuration for a different domain, use the drop down box above<br/>
	</c:when>
	<c:otherwise>
					you <strong>must</strong> change your <strong>token</strong> and <strong>domain</strong> to be the one you want to update<br/>
	</c:otherwise>
</c:choose>

<pre>
:global currentIP;
:local newIP [/ip cloud get public-address];
:if ($newIP != $currentIP) do={
    :log info "IP address $currentIP changed to $newIP";
    :set currentIP $newIP;
    /tool fetch mode=https url="https://www.duckdns.org/update?domains=${dynattrs.exampleSingleDomain}&token=${dynattrs.exampleToken}&ip=$newIP" dst-path=duckdns.txt;
    :local result [/file get duckdns.txt contents];
    :log info "Duck DNS update result: $result";
}
</pre>
add permissions for: read, write, policy, test<br/>
<c:choose>

	<c:when test="${dynattrs.isLoggedIn != 'yes' or dynattrs.exampleSingleDomain == 'exampledomain'}">
		don't forget to change <b>interface name</b>, <b>domains</b> and <b>token</b> according to your configuration!<br/>
	</c:when>
	<c:otherwise>
		don't forget to change <b>interface name</b> according to your configuration!<br/>
	</c:otherwise>
</c:choose>
don't forget to set scheduler!<br/>
don't forget to enable IP Cloud (if you can't enable IP Cloud or are using an earlier version than RouterOS v6.14, use "newIP [/ip address get [find interface="ether1-gateway"];" instead of "newIP [/ip cloud get public-address];" in your script.<br/>
					</div>