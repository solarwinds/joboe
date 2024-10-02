<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>MongoDB test</title>
</head>
<body>

<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	<s:if test="%{extendedOutput != null}">
	<div class="console">
		<s:iterator value="extendedOutput" status="iteratorStatus">
			<s:if test="%{#iteratorStatus.index < 10}">
				<s:property/><br/>
			</s:if>
		</s:iterator>
		<s:if test="%{extendedOutput.size > 10}">
			...<s:property value="%{extendedOutput.size - 10}"/> more document(s)...
		</s:if>
	</div>
	</s:if>
	
	<div class="section">
		<div class="title">Big batch test</div>
		<s:form action="insert-big-batch">
			<s:submit value="Insert big batch" cssClass="button"/>
		</s:form>
		<s:form action="clear-big-batch">
			<s:submit value="Clear big batch" cssClass="button"/>
		</s:form>
		<s:form action="query-in-batch">
			<s:textfield name="batchSize" label="Batch Size"/>
			<s:submit value="Query in batch" cssClass="button"/>
		</s:form>
		<s:form action="query-in-range">
			<s:textfield name="fromIndex" label="From Index"/>
			<s:textfield name="toIndex" label="To Index"/>
			<s:submit value="Query in range" cssClass="button"/>
		</s:form>
	</div>
	
	<div class="section">
		<div class="title">3.0 Operation test (new classes MongoDB and MongoCollection)</div>
		<s:form action="test-all">
			<s:submit value="test-all" cssClass="button"/>
		</s:form>
		<div class="title">Asynchronous Operation test</div>
		<s:form action="test-async-all">
			<s:submit value="test-async-all" cssClass="button"/>
		</s:form>
		<s:form action="test-async-insert">
            <s:submit value="test-async-insert" cssClass="button"/>
        </s:form>
        <div class="title">Reactive Streams test</div>
        <s:form action="test-reactive-all">
            <s:submit value="test-reactive-all" cssClass="button"/>
        </s:form>
        <div class="title">Legacy Operation test (deprecated classes DB and DBCollection)</div>
		<s:form action="test-legacy-all">
			<s:submit value="test-legacy-all" cssClass="button"/>
		</s:form>
		<s:form action="test-aggregate">
			<s:submit value="test-aggregate" cssClass="button"/>
		</s:form>
		<s:form action="test-command">
			<s:submit value="test-command" cssClass="button"/>
		</s:form>
		<s:form action="test-distinct">
			<s:submit value="test-distinct" cssClass="button"/>
		</s:form>
		<s:form action="test-find">
			<s:submit value="test-find" cssClass="button"/>
		</s:form>
		<s:form action="test-find-and-modify">
			<s:submit value="test-find-and-modify" cssClass="button"/>
		</s:form>
		<s:form action="test-map-reduce">
			<s:submit value="test-map-reduce" cssClass="button"/>
		</s:form>
		<s:form action="test-rename">
			<s:submit value="test-rename" cssClass="button"/>
		</s:form>
		<s:form action="test-update-multi">
			<s:submit value="test-update-multi" cssClass="button"/>
		</s:form>
	</div>
	<div class="section">
		<div class="title">Exception handling</div>
		<s:form action="test-invalid-host">
			<s:submit value="Invalid host (localhost:28000)" cssClass="button"/>
		</s:form>
	</div>
	<div class="section">
		<div class="title">Reset the database</div>
		<s:form action="reset-database">
			<s:submit value="Reset" cssClass="button"/>
		</s:form>
	</div>
</div>

</body>
</html>
