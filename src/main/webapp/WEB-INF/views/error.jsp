<%@ page isErrorPage="true" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Error"/>
<%@ include file="_head.jspf" %>
<main class="auth-solo"><div class="card narrow">
  <h1>That did not work</h1>
  <p class="muted">The page you asked for is unavailable, or something went wrong on our side.</p>
  <a class="btn" href="${ctx}/login">Back to Tempo</a>
</div></main>
</body></html>
