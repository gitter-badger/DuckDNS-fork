<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${dynattrs.hasGoodFeedback eq 'true' or dynattrs.hasBadFeedback eq 'true'}">

     	<c:choose>
		<c:when test="${dynattrs.hasGoodFeedback eq 'true'}">
				 <section class="module panels">
					<div class="container mod-notification-pane">
						<div class="col span-12 mod-notification-pane--success">
							<div class="panel mod-notification-pane__title center">
								<span class="h4 mod-notification-pane__title-text"><strong>success:</strong> ${dynattrs.goodFeedback}</span>
							</div>
						</div>
					</div>
				</section>
		</c:when>
		<c:when test="${dynattrs.hasBadFeedback eq 'true'}">
				 <section class="module panels">
					<div class="container mod-notification-pane">
						<div class="col span-12 mod-notification-pane--error">
							<div class="panel mod-notification-pane__title center">
								<span class="h4 mod-notification-pane__title-text"><strong>error:</strong> ${dynattrs.badFeedback}</span>
							</div>
						</div>
					</div>
				</section>
		</c:when>
	</c:choose>

</c:if>