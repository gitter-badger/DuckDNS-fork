<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<script type="text/javascript">
	var signinLink = document.getElementById('signin');
	if (signinLink) {
	  signinLink.onclick = function() { navigator.id.request(); };
	}

	var signoutLink = document.getElementById('signout');
	if (signoutLink) {
	  signoutLink.onclick = function() { navigator.id.logout(); };
	}
	<c:choose>
		<c:when test="${empty dynattrs.personaEmail}">
	var currentUser = null;
		</c:when>
		<c:otherwise>
	var currentUser = '${dynattrs.personaEmail}';
		</c:otherwise>
	</c:choose>

	navigator.id.watch({
	  loggedInUser: currentUser,
	  onlogin: function(assertion) {
	    // A user has logged in! Here you need to:
	    // 1. Send the assertion to your backend for verification and to create a session.
	    // 2. Update your UI.
	    $.ajax({ /* <-- This example uses jQuery, but you can use whatever you'd like */
	      type: 'POST',
	      url: '/login-persona', // This is a URL on your website.
	      data: {code: assertion},
	      success: function(res, status, xhr) { window.location.reload(); },
	      error: function(xhr, status, err) {
	        navigator.id.logout();
	        alert("Login failure: " + err);
	      }
	    });
	  },
	  onlogout: function() {
	    // A user has logged out! Here you need to:
	    // Tear down the user's session by redirecting the user or making a call to your backend.
	    // Also, make sure loggedInUser will get set to null on the next page load.
	    // (That's a literal JavaScript null. Not false, 0, or undefined. null.)
	    $.ajax({
	      type: 'POST',
	      url: '/logout.jsp', // This is a URL on your website.
	      success: function(res, status, xhr) { window.location.reload(); },
	      error: function(xhr, status, err) { alert("Logout failure: " + err); }
	    });
	  }
	});
</script>