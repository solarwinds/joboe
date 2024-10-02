<%@page import="java.util.Map.Entry"%>
<%@page import="com.tracelytics.test.httpclient.Endpoint"%>
<%@page import="java.util.LinkedHashMap"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Test Apache http client 3</title>
<link rel="stylesheet" type="text/css" href="css/style.css"/>
</head>
<body>
<div id="container" >
	<div class="section">
		<div class="title">HTTP Client Test</div>
		<form method="get" action="test-absolute-uri">
			<input type="submit" value="Test absolute URI" class="button"/>
		</form>
		<br/>
		<form method="get" action="test-relative-uri">
			<input type="hidden" name="modifyClient" value="true"/>
			<input type="submit" value="Test relative URI (set host config to client)" class="button"/>
		</form>
		<br/>
		<form method="get" action="test-relative-uri">
			<input type="submit" value="Test relative URI (use host config in execute method)" class="button"/>
		</form>
	</div>
		
	<div>
		<% 
		LinkedHashMap<Endpoint, String> result = (LinkedHashMap<Endpoint, String>)request.getAttribute("result"); 
		if (result != null) {
		    for (Entry<Endpoint, String> entry : result.entrySet()) {
		%>
			
			<h4><%= entry.getKey().toString() %></h4>
			<div class="console"><%= entry.getValue() %></div>
			
		<%
		    }
		}
		%>
	</div>
</div>
</body>
</html>