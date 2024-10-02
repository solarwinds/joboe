<%@page import="com.tracelytics.test.action.DatabaseType"%>
<%@page import="com.tracelytics.test.action.AbstractJdbcAction"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<link rel="stylesheet" type="text/css" href="css/jquery.dataTables.min.css"/>
<script src="scripts/jquery-1.11.3.min.js"></script>
<script src="scripts/jquery.form.min.js"></script>
<script src="scripts/jquery.dataTables.min.js"></script>
<title>JDBC Client test</title>
<script>
function drawDataTable(columns, data) {
	if (columns.length > 0) {
		var oTable = $('#dataTable').DataTable({
    		"aoColumnDefs": columns,
            "aaData": data,
            "searching" : false,
            "bDestroy": true
    	});
	}
}

function submitForm(form) {
	$(form).ajaxSubmit({
        dataType : 'json',
        success : function (response) {
        	drawDataTable(response.columns, response.data);
        	updateConsole(response.message);
        	$("html, body").animate({ scrollTop: 0 }, "slow");
        }
    });
}

function updateConsole(message) {
	$('#console').text(message);
}

function onDatabaseType(type) {
	<% for (DatabaseType type : DatabaseType.values()) { %>
	if (type == '<%= type.name() %>') {
		<% if (type.isEmbedded()) { %>
		$('.extended').attr('disabled','disabled');
		<% } else { %>
		$('.extended').removeAttr('disabled');
		<% } %>
		
		<% if (type.isSupportProcedure()) { %>
		$('#createProcedureSection').show();
		<% } else { %>
		$('#createProcedureSection').hide();
		<% } %>
	}
	<% } %>
}

$(document).ready(function() {
	$('#databaseType').change(function() { onDatabaseType($(this).val());});
	onDatabaseType($('#databaseType').val());
});
</script>
</head>
<body> 

<s:set var="consoleLineMax" value="15"/>

<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	<s:if test="%{extendedOutput != null}">
	<div class="console" id="console">
		<s:iterator value="extendedOutput" status="iteratorStatus">
			<s:if test="%{#iteratorStatus.index < #consoleLineMax}">
				<div><s:property/></div>
			</s:if>
		</s:iterator>
		<s:if test="%{extendedOutput.size > #consoleLineMax}">
			...<s:property value="%{extendedOutput.size - #consoleLineMax}"/> more line(s)...
		</s:if>
	</div>
	</s:if>
	
	<div id="tablePanel">
		<table id="dataTable"></table>
	</div>
	
	<div class="section">
		<div class="title">Database Settings</div>
		<s:form action="initialize-database">
			<s:textfield name="databaseForm.host" label="Host" cssClass="extended"/>
			<s:textfield name="databaseForm.port" label="Port" cssClass="extended"/>
<%-- 			<s:textfield name="databaseForm.type" label="Database Type"/> --%>
			<s:select list="databaseForm.types" name="databaseForm.type" label="Database Type" id="databaseType"/>
			<s:select list="databaseForm.poolings" name="databaseForm.pooling" label="Pooling Type"/>
			<s:textfield name="databaseForm.database" label="Database"/>
			<s:textfield name="databaseForm.user" label="Username" cssClass="extended"/>
			<s:textfield name="databaseForm.password" label="Password" cssClass="extended"/>
			<s:submit value="Initialize" cssClass="button"/>
		</s:form>
		<% if ((Boolean)session.getAttribute("isInitialized") == Boolean.TRUE) { %>
		<div class="title">Query with Statement</div>
		<s:form action="test-statement">
			<s:textarea name="queryForm.query" label="SQL Query" value="SELECT * FROM test_table" cols="60" rows="3"/>
			<s:submit type="button" value="Submit query as Statement" cssClass="button" onclick="submitForm(this.form); return false;"/>
		</s:form>
		<div class="title">Query with PreparedStatement</div>
		<s:form action="test-prepared-statement">
			<s:textarea name="queryForm.query" label="SQL Query" value="SELECT * FROM test_table WHERE last_name LIKE ?" cols="60" rows="3"/>
			<s:textarea name="queryForm.parameters" label="SQL Parameters (separated by space)" value="%Le%" cols="60" rows="1"/>
			<s:submit type="button" value="Submit query as PreparedStatement" cssClass="button" onclick="submitForm(this.form); return false;"/>
		</s:form>
		<div class="title">Query with PreparedStatement with excessive parameters</div>
		<s:form action="test-prepared-statement-with-excessive-parameters">
			<s:submit type="button" value="Submit query as PreparedStatement with excessive parameters" cssClass="button" onclick="submitForm(this.form); return false;"/>
		</s:form>
		<div class="title">Query with PreparedStatement with Blob parameter</div>
		<s:form action="test-blob-statement">
			<s:submit type="button" value="Submit query with Blob parameter" cssClass="button" onclick="submitForm(this.form); return false;"/>
		</s:form>
		<div class="title">Mysql wrapper</div>
		<s:form action="test-mysql-wrapper">
			<s:submit type="button" value="Test Mysql Wrapper" cssClass="button" onclick="submitForm(this.form); return false;"/>
		</s:form>
		<div id="createProcedureSection">
			<div class="title">Create procedure</div>
			<s:form action="test-statement">
				<s:textarea name="queryForm.query" label="SQL Create Procedure" value="CREATE PROCEDURE insertOne(IN i_first_name VARCHAR(255), IN i_last_name VARCHAR(255), OUT o_id INT) BEGIN INSERT INTO test_table(first_name, last_name) VALUES(i_first_name, i_last_name); SELECT MAX(id) INTO o_id FROM test_table; END" cols="60" rows="3"/>
				<s:submit type="button" value="Create Procedure" cssClass="button" onclick="submitForm(this.form); return false"/>
			</s:form>
			<s:form action="test-callable-statement">
				<s:textarea name="queryForm.query" label="SQL Query" value="{CALL insertOne(?, ?, ?)}" cols="60" rows="3"/>
				<s:textarea name="queryForm.parameters" label="SQL IN Parameters (separated by space)" value="Cloud White" cols="60" rows="1"/>
				<s:submit type="button" value="Submit query as CallableStatement" cssClass="button" onclick="submitForm(this.form); return false;"/>
			</s:form>
		</div>
		<% } %>
		
	</div>
</div>

</body>
</html>

