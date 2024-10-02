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
<title>State 2</title>
<link rel="stylesheet" href="<c:url value="/resources/styles/style.css" />" type="text/css" media="screen" />
</head>
<body>
	<div id="container">
		<div class="label">State 2</div>
		<form:form action="${flowExecutionUrl}">
			<button type="submit" name="_eventId_finish">Finish via End State 3</button>
			<button type="submit" name="_eventId_cancel">Cancel</button>
		</form:form>
		<div class="section">
			<ul>
				<li>Finish via End State 3
					<ul>
						<li>Run testService.method4(true), then enter State 3 (End State)</li>
						<li>On Entry of State 3, run testService.method2()</li>
						<li>Restart the flow</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</body>
</html>