<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="My goals"/>
<%@ include file="../_head.jspf" %>
<header class="topbar">
  <span class="logo">Tempo</span>
  <span>Hi, <strong><c:out value="${sessionScope.user.name}"/></strong> &nbsp;<a href="${ctx}/logout">Sign out</a></span>
</header>
<main class="wrap">
  <%@ include file="../_flash.jspf" %>

  <c:if test="${not empty reminders}">
    <div class="flash warn"><strong>Deadlines coming up</strong>
      <ul><c:forEach var="r" items="${reminders}"><li><c:out value="${r}"/></li></c:forEach></ul>
    </div>
  </c:if>

  <section class="card">
    <h2>Progress summary</h2>
    <div class="kpis">
      <div><b>${summary.total}</b><span>goals</span></div>
      <div><b>${summary.completed}</b><span>completed</span></div>
      <div><b>${summary.overdue}</b><span>overdue</span></div>
      <div><b>${summary.averagePercent}%</b><span>average progress</span></div>
      <div><b><fmt:formatNumber value="${summary.totalHours}" maxFractionDigits="1"/>h</b><span>time logged</span></div>
    </div>
    <c:if test="${not empty summary.byType}">
      <p class="muted">By type:
        <c:forEach var="e" items="${summary.byType}"><span class="chip"><c:out value="${e.key}"/> &times; ${e.value}</span></c:forEach>
      </p>
    </c:if>
  </section>

  <div class="grid2">
    <form class="card" method="post" action="${ctx}/user/goal/add">
      <h2>Set a goal</h2>
      <label>Goal name<input type="text" name="name" maxlength="100" placeholder="e.g. Finish Java project" required></label>
      <label>Goal type
        <select name="typeId" required>
          <c:forEach var="t" items="${types}"><option value="${t.id}"><c:out value="${t.name}"/> (max ${t.maxTargetHours}h)</option></c:forEach>
        </select>
      </label>
      <label>Target (hours)<input type="number" name="targetHours" min="0.5" step="0.5" required></label>
      <label>Deadline<input type="date" name="deadline" required></label>
      <button class="btn" type="submit">Add goal</button>
    </form>

    <form class="card" method="post" action="${ctx}/user/log/add">
      <h2>Log time</h2>
      <c:choose>
        <c:when test="${empty activeGoals}"><p class="muted">Add a goal first, then log time against it.</p></c:when>
        <c:otherwise>
          <label>Goal
            <select name="goalId" required>
              <c:forEach var="g" items="${activeGoals}"><option value="${g.id}"><c:out value="${g.name}"/></option></c:forEach>
            </select>
          </label>
          <label>Start<input type="datetime-local" name="start" required></label>
          <label>End<input type="datetime-local" name="end" required></label>
          <button class="btn" type="submit">Save entry</button>
        </c:otherwise>
      </c:choose>
    </form>
  </div>

  <section class="card">
    <h2>My goals</h2>
    <c:if test="${empty goals}"><p class="muted">No goals yet. Set your first one above.</p></c:if>
    <c:if test="${not empty goals}">
    <div class="scroll"><table>
      <thead><tr><th>Goal</th><th>Type</th><th>Deadline</th><th>Progress</th><th>Status</th><th></th></tr></thead>
      <tbody>
      <c:forEach var="g" items="${goals}">
        <tr>
          <td><c:out value="${g.name}"/></td>
          <td><c:out value="${g.typeName}"/></td>
          <td>${g.deadline}</td>
          <td class="progress-cell">
            <div class="bar ${g.completed ? 'done' : ''}"><span style="width:${g.progressPercent}%"></span></div>
            <small><fmt:formatNumber value="${g.loggedHours}" maxFractionDigits="1"/> / ${g.targetHours} h (${g.progressPercent}%)</small>
          </td>
          <td>
            <c:choose>
              <c:when test="${g.completed}"><span class="tag ok">Completed</span></c:when>
              <c:when test="${g.overdue}"><span class="tag bad">Overdue</span></c:when>
              <c:otherwise><span class="tag">${g.daysLeft} days left</span></c:otherwise>
            </c:choose>
          </td>
          <td><form method="post" action="${ctx}/user/goal/delete" onsubmit="return confirm('Delete this goal and its time logs?')">
            <input type="hidden" name="id" value="${g.id}"><button class="link danger" type="submit">Delete</button></form></td>
        </tr>
      </c:forEach>
      </tbody>
    </table></div>
    </c:if>
  </section>

  <section class="card">
    <h2>Time logs</h2>
    <c:if test="${empty logs}"><p class="muted">Nothing logged yet.</p></c:if>
    <c:if test="${not empty logs}">
    <div class="scroll"><table>
      <thead><tr><th>Goal</th><th>Start</th><th>End</th><th>Duration</th><th></th></tr></thead>
      <tbody>
      <c:forEach var="t" items="${logs}">
        <tr>
          <td><c:out value="${t.goalName}"/></td><td>${t.startDisplay}</td><td>${t.endDisplay}</td><td>${t.durationDisplay}</td>
          <td><form method="post" action="${ctx}/user/log/delete"><input type="hidden" name="id" value="${t.id}">
            <button class="link danger" type="submit">Remove</button></form></td>
        </tr>
      </c:forEach>
      </tbody>
    </table></div>
    </c:if>
  </section>
</main>
</body></html>
