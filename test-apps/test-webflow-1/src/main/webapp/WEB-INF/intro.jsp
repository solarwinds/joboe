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
		<div class="label">Please select one of the below Flows:</div>
		<br/>
		<button onclick="location.href='plain/test'">Simple flow</button>
		<button onclick="location.href='ajax/flowContainer'">Ajax flow (embedded)</button>
		
		<div class="section">
			<div class="label">What is Spring Web Flow?</div>
			<br/>
			<div class="text">
			Spring Web Flow is a sub-project of Spring Framework that allows developer to extract the "navigation" logics and manipulate/present them in XMLs.
			It is mostly used for web-sites with well-defined flows. A flow could simply be something like booking a movie ticket online, that a user would go through
			several steps for example:
			<ol>
				<li>User input credit card information</li>
				<li>User confirm information</li>
				<li>System process and charge from the credit card</li>
				<li>User sees the Confirmation screen</li>
			</ol>	
			Each of those steps could be treated as a "State" in web-flow term. Developer might also define "Action"s to be performed before/during/after each flow<br/>
			
			You might find more informations following these links: <br/>
			<a href="http://refcardz.dzone.com/refcardz/spring-web-flow">Short Summary</a><br/>
			<a href="http://static.springsource.org/spring-webflow/docs/2.3.x/reference/html/index.html">Detailed Explanations</a><br/>
			<br/>
			Spring Web Flow also uses a technique known as <a href="http://www.ervacon.com/products/swf/tips/tip4.html">Post-Redirect-Get</a> to handle Http requests by default
			</div>
			
			<br/><br/>
			
			<div class="label">What do we currently track?</div>
			<div class="text">
			<ul>
				<li>Webflow layer entry/exit</li>
				<li>State enter/exit/refresh (transition-on, flow Id, State id, State type)</li>
				<li>Action start/finish (actions defined in the "flow" XML)</li>
				<li>Exception handled by webflow</li>
				<li>View rendering (view name, jsp path etc)</li>
			</ul>
			</div>
		
		</div>
	</div>
	</body>
</html>

