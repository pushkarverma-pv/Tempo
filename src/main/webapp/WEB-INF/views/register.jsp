<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create account"/>
<%@ include file="_head.jspf" %>
<main class="auth-solo">
  <form method="post" action="${ctx}/register" class="card narrow">
    <h2>Create your account</h2>
    <c:if test="${not empty error}"><div class="flash error"><c:out value="${error}"/></div></c:if>
    <label>Full name<input type="text" name="name" maxlength="100" required></label>
    <label>Email<input type="email" name="email" required></label>
    <label>Password (min 6 characters)<input type="password" name="password" minlength="6" required></label>
    <label>Confirm password<input type="password" name="confirm" minlength="6" required></label>
    <button class="btn" type="submit">Create account</button>
    <p class="muted">Already registered? <a href="${ctx}/login">Sign in</a></p>
  </form>
</main>
</body></html>
