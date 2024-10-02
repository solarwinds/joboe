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
		
		<div class="section">
			<ul>
				<li>On render of State 2, run testService.method2()</li>
			</ul>
		</div>
		<form:form action="${flowExecutionUrl}">
			<button type="submit" name="_eventId_next">Goto State 5</button>
			<button type="submit" name="_eventId_back">Back to State 1</button>
			<button type="submit" name="_eventId_subflow">Start Sub-flow</button>
			<button type="submit" name="_eventId_exception">Trigger Exception</button>
			<button type="submit" name="_eventId_cancel">Cancel</button>
		</form:form>
		<div class="section">
			<ul>
				<li>Goto State 5
					<ul>
						<li>Set flowScope.testingAttribute with result of testService.method3('dummy')</li>
						<li>Run testService.method4(true), then enter State 3 (Action State)</li>
						<li>On Entry of State 3, run testService.method2()</li>
						<li>In State 3, run testService.method1()</li>
						<li>In State 3, run testService.method3('proceed')</li>
						<li>On Exit of State 3, run testService.method2()</li>
						<li>Enter State 4 (Decision State)</li>
						<li>In State 4, run testService.method4(true)</li>
						<li>Enter State 5 (View State)</li>
					</ul>
				</li>
				<li>Back to State 1
					<ul>
						<li>Run testService.method4(true), then enter State 1 (View State)</li>
					</ul>
				</li>
				<li>Start Sub-flow
					<ul>
						<li>Run testService.method4(true), then enter State subflowState (Sub-flow State)</li>
						<li>Enter State subflowState1 (View State)</li>
					</ul>
				</li>
				<li>Trigger Exception
					<ul>
						<li>Trigger an exception and the flow will be interrupted</li>
					</ul>
				</li>
			</ul>
		</div>
	</div>
</body>
</html>