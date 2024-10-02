<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>State 2</title>
</head>
<body>
	<h5>State 2</h5>
	<form:form action="${flowExecutionUrl}">
		<button type="submit" name="_eventId_finsih">Finish via State 3</button>
		<button type="submit" name="_eventId_back">Back</button>
		<button type="submit" name="_eventId_cancel">Cancel</button>
	</form:form>
</body>
</html>

<div id="embeddedFlow">
	<p class="notice">This is step 2 of the embedded flow</p>
	<form id="step2" action="${flowExecutionUrl}" method="POST">
		<button id="cancel" type="submit" name="_eventId_cancel">Cancel</button>
		<button id="previous" type="submit" name="_eventId_back">Back</button>
		<button id="finish" type="submit" name="_eventId_finish">Finish via State 3</button>
		<script type="text/javascript">
			Spring.addDecoration(new Spring.AjaxEventDecoration({elementId:'finish',event:'onclick',formId:'step2',params:{fragments:"body"}}));
			Spring.addDecoration(new Spring.AjaxEventDecoration({elementId:'previous',event:'onclick',formId:'step2',params:{fragments:"body"}}));
			Spring.addDecoration(new Spring.AjaxEventDecoration({elementId:'cancel',event:'onclick',formId:'step2',params:{fragments:"body"}}));
		</script>
	</form>
</h5>