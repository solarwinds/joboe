<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=9" />
<title>Test Web flow</title>
<link rel="stylesheet" href="<c:url value="/resources/styles/style.css" />" type="text/css" media="screen" />
</head>
<body>
	<div id="container">
		<div class="section">
			<div class="label">
				FlowControll test
			</div>
			<br/>
			<div class="text">
				This is a test that uses the org.springframework.webflow.mvc.servlet.FlowController. Some older implementation (converted from SWF 1) might use this controller instead of org.springframework.webflow.mvc.servlet.FlowHandlerAdapter</h5>
			</div>
		</div>
		<button onclick="location.href='controller/test.htm'">Simple flow</button>
	</div>
</body>
</html>