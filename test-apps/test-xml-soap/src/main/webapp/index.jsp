<%-- <%@page import="com.tracelytics.api.RUMWrapper" %> --%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Weather SOAP test</title>
<%-- <% out.println(RUMWrapper.getHeader()); %> --%>
</head>
<body>

<form action="weather.do">
Zip code: 
	<input type="text" name="zip" maxlength="5" size="10"/>
	<input type="submit" value="Check Forecasts!"/>
</form>

<%
	String city = (String)request.getAttribute("city");
	String responseXml = (String)request.getAttribute("response");

	if (responseXml != null) {
%>
	<div>Response XML: </div>
	<div><%= responseXml %></div>	    
<%
	} else {
%>
	invalid zip!
<%
	}
%>

<%-- <% out.println(RUMWrapper.getFooter()); %> --%>
</body>
</html>