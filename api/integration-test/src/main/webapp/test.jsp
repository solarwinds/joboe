<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.appoptics.api.ext.RUM"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<% out.println(RUM.getHeader()); %>
<title>Hello World!</title>
</head>
<body>
    <h2>Hello!  This is just a simple test page.  If you can see this, it works.</h2>
<% out.println(RUM.getFooter()); %>
</body>
</html>
