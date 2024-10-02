<%@page import="java.util.List"%>

<%-- <%@page import="com.tracelytics.api.RUMWrapper" %> --%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Simple REST/SOAP test</title>
</head>
<body>

<form action="sampleSoap.do">
	<input type="submit" value="Test local Sample SOAP server"/>
</form>

<form action="sampleSoapAsync.do">
	<input type="submit" value="Test local Sample SOAP server (Async)"/>
</form>

<form action="sampleRest.do">
	<input type="submit" value="Test local Sample REST server"/>
</form>

<form action="sampleRestAsync.do">
	<input type="submit" value="Test local Sample REST server (Async)"/>
</form>

<form action="apiRest.do">
	<input type="submit" value="Test TraceView API (REST)"/>
</form>

<form action="exceptionRest.do">
	<input type="submit" value="Test Exceptions from Server (REST)"/>
</form>

<% 
	String clientAction = (String)request.getAttribute("clientAction");
	String failureMessage = (String)request.getAttribute("failureMessage");
	if (clientAction != null) {
%>
	<div>Test successful! Sent requests for <b><%= clientAction %></b></div>
<% 
	} else if (failureMessage != null) {
%>	
	<div>Test failed! <b><%= failureMessage %></b></div>
<%
	}
%>
</body>
</html>