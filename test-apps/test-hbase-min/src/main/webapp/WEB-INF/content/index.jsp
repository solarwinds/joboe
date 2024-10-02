<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>HBase test</title>
</head>
<body>

<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	<div class="section">
		<div class="title">HBase Admin Operation test</div>
		<s:form action="test-table">
			<s:submit value="Test Create and Drop table " cssClass="button"/>
		</s:form>
		<s:form action="test-column">
			<s:submit value="Test Create and Delete column" cssClass="button"/>
		</s:form>
	</div>
	
	<div class="section">
		<div class="title">HBase Table Operation test with parameters</div>
		
		<s:form action="test-put-rows">
			<s:textfield name="count" label="Number of rows" maxlength="6"/>
			<s:submit value="Test Put rows" cssClass="button"/>
		</s:form>
		<s:form action="test-delete-rows">
			<s:textfield name="count" label="Number of rows" maxlength="6"/>
			<s:submit value="Test Delete rows" cssClass="button"/>
		</s:form>
	</div>
	
	
	
	<div class="section">
		<div class="title">HBase Table Operation test</div>
		<s:form action="test-put-no-batch">
			<s:submit value="Test Put (no batching - 30 entries)" cssClass="button"/>
		</s:form>
		<s:form action="test-put-in-batch">
			<s:submit value="Test Put (batching - 30 entries)" cssClass="button"/>
		</s:form>
		<s:form action="test-big-put">
			<s:submit value="Test Put with big row (30 entries - 1M each))" cssClass="button"/>
		</s:form>
		<s:form action="test-big-delete">
			<s:submit value="Test Delete with big row (30 entries - 1M each))" cssClass="button"/>
		</s:form>
		<s:form action="test-bulk-get">
			<s:submit value="Test Get (multi)" cssClass="button"/>
		</s:form>
		<s:form action="test-scanner">
			<s:submit value="Test Scanner" cssClass="button"/>
		</s:form>
		<s:form action="test-batch">
			<s:submit value="Test Batch" cssClass="button"/>
		</s:form>
		<s:form action="test-check-and-operation">
			<s:submit value="Test CheckAndPut and CheckAndDelete" cssClass="button"/>
		</s:form>
		<s:form action="test-coprocessor-exec">
			<s:submit value="Test CoprocessorExec" cssClass="button"/>
		</s:form>
		<s:form action="test-delete">
			<s:submit value="Test Delete" cssClass="button"/>
		</s:form>
		<s:form action="test-exists">
			<s:submit value="Test Exists" cssClass="button"/>
		</s:form>
		<s:form action="test-get-row-or-before">
			<s:submit value="Test GetRowOrBefore" cssClass="button"/>
		</s:form>
		<s:form action="test-increment">
			<s:submit value="Test Increment" cssClass="button"/>
		</s:form>
	</div>
	<div class="section">
		<div class="title">HBase multi-action test on thread pool</div>
		<b>To verify the trace, inspect the raw view and make sure the async extends (round trip to server) are evenly distributed to GET operation. If the
		asyn extends are all pointing at a single GET operation, then the instrumentation is not working properly</b>
		<s:form action="test-multiple-thread">
			<s:submit value="Test Multiple Thread" cssClass="button"/>
		</s:form>
	</div>
</div>


</body>
</html>
