<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Runtime exception</title>
</head>
<body>
	<% 
	//compilation error, trigger unreachable code
		throw new RuntimeException("JOboe testing exception thrown from JSP");
	
	%>
</body>
</html>