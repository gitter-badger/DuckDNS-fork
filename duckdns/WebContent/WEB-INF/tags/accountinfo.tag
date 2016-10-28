<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class="container outer-container">
	<div class="one-true white gpl">
		<div class="col span-4">
			<div class="panel white logo-holder">
				<p class="ee-masthead__logo ${dynattrs.accountIcon}">
				</p>
			</div>
		</div>
		<div class="col span-7">
			<div class="panel white" style="background-color: transparent;">
				<h1 class="center">Duck DNS</h1>	
				<c:choose>
					<c:when test="${dynattrs.isLoggedIn eq 'true' and dynattrs.section eq 'index'}">
						<div class="static-data static-data-border">
							<dl>
								<dt>account</dt>
								<dd>${dynattrs.sessionEmail}</dd>
								<dt>type</dt>
								<dd>${dynattrs.accountType}</dd>
								<dt>token</dt>
								<dd>${dynattrs.accountToken}</dd>
								<dt>token generated</dt>
								<dd>${dynattrs.tokenGenerated}</dd>
								<dt>created date</dt>
								<dd>${dynattrs.createdDate}</dd>
							</dl>
						</div>
					</c:when>
					<c:when test="${dynattrs.section ne 'index'}">
						<div class="news-items">
							<!-- NEWS:START -->
							<h2 class="center">free dynamic DNS hosted on Amazon VPC</h2>
							<p class="center">
								<strong>new:</strong> <a style="color:#cccccc;text-decoration:underline;" href="faqs.jsp">GNU GPLv3 licensed</a> <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://drive.google.com/folderview?id=0B-jet_9PYdwyYXNzVnIyMVI4NG8&usp=sharing">source</a><br/>
								<strong>new:</strong> <a style="color:#cccccc;text-decoration:underline;" href="install.jsp?tab=osx-ip-monitor">osx IP Monitor updater</a> - supports IPv6<br/>
								<strong>new:</strong> become a <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://www.patreon.com/user?u=3209735&ty=h&u=3209735">Patreon</a><br/>
							</p>
							<!-- NEWS:END -->
						</div>
					</c:when>
					<c:otherwise>
						<!-- NEWS:START -->
						<h2 class="center">free dynamic DNS hosted on Amazon VPC</h2>
						<p class="center">
							<strong>new:</strong> <a style="color:#cccccc;text-decoration:underline;" href="faqs.jsp">GNU GPLv3 licensed</a> <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://drive.google.com/folderview?id=0B-jet_9PYdwyYXNzVnIyMVI4NG8&usp=sharing">source</a><br/>
							<strong>new:</strong> <a style="color:#cccccc;text-decoration:underline;" href="install.jsp?tab=osx-ip-monitor">osx IP Monitor updater</a> - supports IPv6<br/>
							<strong>new:</strong> become a <a target="new" style="color:#cccccc;text-decoration:underline;" href="https://www.patreon.com/user?u=3209735&ty=h&u=3209735">Patreon</a><br/>
						</p>
						<!-- NEWS:END -->
					</c:otherwise>
				</c:choose>
			</div>
		</div>
	</div>
</div>
