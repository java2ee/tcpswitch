<%@
 page import="java.util.List"%><%@
 page import="java.util.ArrayList"%><%@ 
 page import="ru.transset.app.tcpswitch.TCPNodeAdapter"%><%@ 
 page import="ru.transset.app.tcpswitch.bean.SwitchBean"%><%@ 
 page import="ru.transset.app.tcpswitch.bean.NodeBean"%><%@
 page import="java.util.Enumeration"%><%@
 page import="ru.transset.app.tcpswitch.TCPNode"%><%@ 
 page import="java.util.Iterator"%><%@ 
 page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<jsp:useBean id='Manager' scope='application' class='ru.transset.app.tcpswitch.TCPManager'/>
<jsp:useBean id='JmxKafka' scope='application' class='ru.transset.kafka.jmx.JmxKafka'/>
<%
String ip = request.getRemoteAddr();
String user = request.getRemoteUser();
String name = request.getParameter("off");
String app = request.getParameter("app");
try {
	if (app != null) {
		if (name != null) {
			Manager.off(app, name, ip, user);
			response.sendRedirect("index.jsp");
			return;
		} else {
			name = request.getParameter("on");
			if (name != null) {
				Manager.on(app, name, ip, user);
				response.sendRedirect("index.jsp");
				return;
			}
		}
	}
} catch (Exception e) {
}
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Manager of groups TCP/IP tunnels</title>
<script type="text/javascript">
   function errorWindow(message) {
    alert(message);
   }
</script>
</head>
<body>
<h1>Управление группами переключателей TCP/IP туннелей</h1>
Узел автоматического управления: <%= JmxKafka.getMainName() %>
<%
for (Enumeration<String> enumeration = Manager.getNameNodes(); enumeration.hasMoreElements(); ) {
	String nameNode = enumeration.nextElement();
	TCPNodeAdapter node = Manager.getTCPNodeAdapter(nameNode);
	NodeBean bean = node.getNodeBean();
	SwitchBean[] columns = bean.columns();
	String[] rows = bean.rows();
	out.print("<h2>");
	out.print(bean.getDescription());
	out.println("</h2>");
	out.println("<table border=\"1\">");
	out.println("<tr><th>Имя</th><th>Команда</th>");
	// Вычислить текущие группы, в нормальной ситуации на всех узлах должна быть одна и та же группа - size() == 1
	List<String> currents = new ArrayList<String>();
	for (int index = 0; index < columns.length; index++) {
		SwitchBean column = columns[index];
		if (column.isStatus()) {
			out.print("<th><font color=\"");
			out.print("grean");
		} else {
			out.print("<th onmouseover=\"errorWindow('");
			out.print(column.getError());
			out.print("')\"><font color=\"");
			out.print("red");
		}
		out.print("\">");
		out.print(column.getAdapter());
		out.print("</font></th>");
		if (!currents.contains(column.getCurrent())) currents.add(column.getCurrent());
	}
	String current;
	if (currents.size() == 1) current = currents.get(0);
	else current = null;
	out.println("</tr>");
	for (int index = 0; index < rows.length; index++) {
		String row = rows[index];
		out.print("<tr><td>");
		out.print(row);
		out.print("</td><td><a href=\"index.jsp?");
		out.print(row.equals(current) ? "off" : "on");
		out.print("=");
		out.print(row);
		out.print("&app=");
		out.print(nameNode);
		out.print("\">");
		out.print(row.equals(current) ? "stop" : "start");
		out.print("</a></td>");
		for (int indexColumn = 0; indexColumn < columns.length; indexColumn++) {
			SwitchBean column = columns[indexColumn];
			out.print("<td>");
			out.print(row.equals(column.getCurrent()) ? "on" : "");
			//out.print(header.getCurrent());
			out.print("</td>");
		}
		out.println("</tr>");
	}
	out.println("</table>");
}
%>
</body>
</html>
