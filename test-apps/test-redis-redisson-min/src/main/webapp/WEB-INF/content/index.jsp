<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>Redis Redisson Client test</title>
</head>
<body>

<s:set var="consoleLineMax" value="15"/>

<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	<s:if test="%{extendedOutput != null}">
	<div class="console">
		<s:iterator value="extendedOutput" status="iteratorStatus">
			<s:if test="%{#iteratorStatus.index < #consoleLineMax}">
				<s:property/><br/>
			</s:if>
		</s:iterator>
		<s:if test="%{extendedOutput.size > #consoleLineMax}">
			...<s:property value="%{extendedOutput.size - #consoleLineMax}"/> more line(s)...
		</s:if>
	</div>
	</s:if>
	
	<div class="section">
		<div class="title">Operation test</div>
		<s:form action="test-all">
			<s:submit value="test all operations" cssClass="button"/>
		</s:form>
		<s:form action="test-multi">
			<s:submit value="test multi (transaction)" cssClass="button"/>
		</s:form>
		<s:form action="test-sync">
			<s:submit value="test Synchronous Connection" cssClass="button"/>
		</s:form>
		<s:form action="test-async">
			<s:submit value="test Asynchronous Connection" cssClass="button"/>
		</s:form>
		<s:form action="test-get">
			<s:submit value="test GET" cssClass="button"/>
		</s:form>
		<s:form action="test-set">
			<s:submit value="test SET" cssClass="button"/>
		</s:form>
	</div>
	
	<div class="section">
		<div class="title">Re-initialize the DB</div>
		<s:form action="reset">
			<s:submit value="re-initialize" cssClass="button"/>
		</s:form>
	</div>
</div>

</body>
</html>
