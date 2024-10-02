<%@page import="java.util.Map.Entry"%>
<%@page import="com.tracelytics.test.httpclient.Target"%>
<%@page import="java.util.Map"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Test Apache http client 4</title>
<link rel="stylesheet" type="text/css" href="css/style.css"/>
</head>
<body>
<div id="container" >
	<div class="section">
		<div class="title">HTTP Client Test</div>
		<form method="get" action="test">
			<input type="hidden" name="type" value="sync"/>
		    <input type="submit" value="Test URI (Synchronous)" class="button"/>
		</form>
		<form method="get" action="test">
			<input type="hidden" name="type" value="async"/>
			<input type="submit" value="Test URI (Asynchronous)" class="button"/>
		</form>
	</div>
		
	<div>
		<% 
		Map<Target, String> result = (Map<Target, String>)request.getAttribute("result");
		if (result != null) {
		    for (Entry<Target, String> entry : result.entrySet()) {
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