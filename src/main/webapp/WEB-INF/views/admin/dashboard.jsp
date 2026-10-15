<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Admin"/>
<%@ include file="../_head.jspf" %>
<header class="topbar">
  <span class="logo">Tempo <small>admin</small></span>
  <span><c:out value="${sessionScope.user.name}"/> &nbsp;<a href="${ctx}/logout">Sign out</a></span>
</header>
<main class="wrap">
  <%@ include file="../_flash.jspf" %>

  <section class="card">
    <h2>Tool usage</h2>
    <div class="kpis">
      <div><b>${users.size()}</b><span>accounts</span></div>
      <div><b>${goalCount}</b><span>goals set</span></div>
      <div><b>${completedCount}</b><span>goals completed</span></div>
      <div><b>${totalHours}h</b><span>total time logged</span></div>
    </div>
    <div class="grid2 charts">
      <div><h3>Hours logged, last 7 days</h3><canvas id="dayChart" height="170"></canvas></div>
      <div><h3>Hours logged by user</h3><canvas id="userChart" height="170"></canvas></div>
    </div>
  </section>

  <div class="grid2">
    <section class="card">
      <h2>Goal setting parameters</h2>
      <form method="post" action="${ctx}/admin/type/add">
        <label>Goal type<input type="text" name="name" maxlength="60" placeholder="e.g. Reading" required></label>
        <label>Tracking metric<input type="text" name="metric" maxlength="100" placeholder="e.g. Hours of reading" required></label>
        <label>Max target hours<input type="number" name="maxHours" min="1" max="2000" value="200" required></label>
        <button class="btn" type="submit">Save goal type</button>
      </form>
      <div class="scroll"><table>
        <thead><tr><th>Type</th><th>Metric</th><th>Max h</th><th></th></tr></thead>
        <tbody><c:forEach var="t" items="${types}">
          <tr><td><c:out value="${t.name}"/></td><td><c:out value="${t.metric}"/></td><td>${t.maxTargetHours}</td>
            <td><form method="post" action="${ctx}/admin/type/delete" onsubmit="return confirm('Remove this goal type?')">
              <input type="hidden" name="id" value="${t.id}"><button class="link danger" type="submit">Remove</button></form></td></tr>
        </c:forEach></tbody>
      </table></div>
    </section>

    <section class="card">
      <h2>Add an account</h2>
      <form method="post" action="${ctx}/admin/user/add">
        <label>Name<input type="text" name="name" maxlength="100" required></label>
        <label>Email<input type="email" name="email" required></label>
        <label>Temporary password<input type="password" name="password" minlength="6" required></label>
        <label>Role<select name="role"><option value="USER">User</option><option value="ADMIN">Admin</option></select></label>
        <button class="btn" type="submit">Create account</button>
      </form>
    </section>
  </div>

  <section class="card">
    <h2>User accounts</h2>
    <div class="scroll"><table>
      <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
      <tbody><c:forEach var="u" items="${users}">
        <tr>
          <td><c:out value="${u.name}"/></td><td><c:out value="${u.email}"/></td><td>${u.role}</td>
          <td><span class="tag ${u.active ? 'ok' : 'bad'}">${u.active ? 'Active' : 'Deactivated'}</span></td>
          <td class="actions">
            <c:if test="${u.id != sessionScope.user.id}">
              <form method="post" action="${ctx}/admin/user/toggle"><input type="hidden" name="id" value="${u.id}">
                <button class="link" type="submit">${u.active ? 'Deactivate' : 'Reactivate'}</button></form>
              <form method="post" action="${ctx}/admin/user/delete" onsubmit="return confirm('Delete this user and ALL their goals and logs?')">
                <input type="hidden" name="id" value="${u.id}"><button class="link danger" type="submit">Delete</button></form>
            </c:if>
          </td>
        </tr>
      </c:forEach></tbody>
    </table></div>
  </section>
</main>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script>
  const byDay = ${chartByDay}, byUser = ${chartByUser};
  const opts = { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } };
  new Chart(document.getElementById('dayChart'), { type: 'line',
    data: { labels: byDay.labels.map(d => d.slice(5)), datasets: [{ data: byDay.values, borderColor: '#0B7A75', backgroundColor: 'rgba(11,122,117,.15)', fill: true, tension: .3 }] }, options: opts });
  new Chart(document.getElementById('userChart'), { type: 'bar',
    data: { labels: byUser.labels, datasets: [{ data: byUser.values, backgroundColor: '#14304F' }] }, options: opts });
</script>
</body></html>
