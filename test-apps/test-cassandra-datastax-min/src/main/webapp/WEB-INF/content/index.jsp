<%@page import="com.tracelytics.test.action.AbstractCqlAction"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>Cassandra Datastax test</title>
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
		<s:form action="test-query">
			<s:submit value="test SELECT queries" cssClass="button"/>
		</s:form>
		<s:form action="test-query-async">
			<s:submit value="test asynchronous SELECT queries" cssClass="button"/>
		</s:form>
		<s:form action="test-update">
			<s:submit value="test UPDATE in bound statement with null" cssClass="button"/>
		</s:form>
		<s:form action="test-prepared-statement-with-all-data-types">
			<s:submit value="test prepared statement with all data types" cssClass="button"/>
		</s:form>
		<s:form action="test-bound-statement-with-all-data-types">
			<s:submit value="test bound statement with all data types" cssClass="button"/>
		</s:form>
		<s:form action="test-long-query">
			<s:submit value="test insert %{@com.tracelytics.test.action.TestLongQuery@COUNT} records (long query)" cssClass="button"/>
		</s:form>
		<s:form action="test-use-keyspace">
			<s:submit value="test using specific keyspace in the session" cssClass="button"/>
		</s:form>
		<s:form action="test-session-with-keyspace">
			<s:submit value="test session instantiation with specific keyspace" cssClass="button"/>
		</s:form>
		<s:form action="test-exception">
			<s:submit value="test exception in execute" cssClass="button"/>
		</s:form>
		<s:form action="test-exception-async">
			<s:submit value="test exception in executeAsync" cssClass="button"/>
		</s:form>


		<div class="title">Query test</div> 
		<s:form action="test-execute">
			Enter query below (default table : <%= AbstractCqlAction.TEST_TABLE %>)
			<s:textarea label="" labelSeparator="" name="statement" cssStyle="width : 500px; height: 100px;" value="%{statement == null ? 'SELECT * FROM test_keyspace.test_table' : statement}"/>
			<s:submit value="execute custom query" cssClass="button"/>
		</s:form>
		
		<div class="title">Change hosts that client connects to</div>
		<div> 
		Current host(s):<br/>
		<ul>
		<% for (String host : AbstractCqlAction.getCurrentHosts()) { %>
		    <li><%= host %></li>
		<% }  %>
		</ul>
		</div>
		 
		<s:form action="change-hosts">
			Enter host(s) below (for multiple hosts, please separate them by semicolon ;)
			<s:textarea label="" labelSeparator="" name="hostString" cssStyle="width : 300px; height: 30px;"/>
			<s:submit value="change hosts" cssClass="button"/>
		</s:form>
	</div>

</div>

</body>
</html>
