<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>Ehcache test</title>
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
		<s:form action="test-misc">
			<s:submit value="test miscellenous operations" cssClass="button"/>
		</s:form>
		<s:form action="test-put">
			<s:submit value="test PUT operations" cssClass="button"/>
		</s:form>
		<s:form action="test-get">
			<s:submit value="test GET operations" cssClass="button"/>
		</s:form>
		<s:form action="test-load">
			<s:submit value="test LOAD operations" cssClass="button"/>
		</s:form>
		<s:form action="test-remove">
			<s:submit value="test REMOVE operations" cssClass="button"/>
		</s:form>
		<s:form action="test-replace">
			<s:submit value="test REPLACE operations" cssClass="button"/>
		</s:form>
		<s:form action="test-lock">
			<s:submit value="test LOCK operations" cssClass="button"/>
		</s:form>
		
	</div>
	
	<div class="section">
		<div class="title">Query/Search API</div>
		<s:form action="test-query">
			<s:submit value="test SELECT queries" cssClass="button"/>
		</s:form>
	</div>
	
	
	<div class="section">
		<div class="title">Hibernate 2nd level cache</div>
		<s:form action="test-hibernate">
			<s:submit value="test Hibernate operations" cssClass="button"/>
		</s:form>
	</div>
	
		<div class="section">
		<div class="title">Web page cache filter</div>
		<a href="cached_page">sample page</a>
	</div>
	

</div>

</body>
</html>
