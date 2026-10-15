<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign in"/>
<%@ include file="_head.jspf" %>
<main class="auth">
  <section class="auth-brand">
    <p class="logo">Tempo</p>
    <h1>Set the goal.<br>Log the hours.<br>See the progress.</h1>
    <p class="muted-light">A personal time and goal tracker. Every minute you log moves a goal closer to done.</p>
  </section>
  <section class="auth-form">
    <form method="post" action="${ctx}/login" class="card">
      <h2>Sign in</h2>
      <c:if test="${not empty error}"><div class="flash error"><c:out value="${error}"/></div></c:if>
      <c:if test="${not empty success}"><div class="flash ok"><c:out value="${success}"/></div></c:if>
      <label>Email<input type="email" name="email" required autofocus></label>
      <label>Password<input type="password" name="password" required></label>
      <button class="btn" type="submit">Sign in</button>
      <p class="muted">New here? <a href="${ctx}/register">Create an account</a></p>
    </form>
  </section>
</main>
</body></html>
