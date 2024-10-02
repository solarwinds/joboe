<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>HttpURLConnection Test</title>
<script type="text/javascript">
	function submitForm(formId, action) {
		var form = document.getElementById(formId);
		form.action = action;
		form.submit();
	}
</script>
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
		<s:form id="testForm">
			<div class="title">Operation test</div>
			<s:textfield name="urlString" label="Target URL"/>
			<s:checkbox name="callConnect" label="Call Connect"></s:checkbox>
			<s:radio label="read input method" name="inputMethodString" list="inputMethodStrings" value="defaultInputMethodString"/>
		</s:form>
		<div class="button" onclick="submitForm('testForm', 'test-get')">Test GET</div>
		<div class="button" onclick="submitForm('testForm', 'test-post')">Test POST</div>
		<div class="button" onclick="submitForm('testForm', 'test-delete')">Test DELETE</div>
		<div class="button" onclick="submitForm('testForm', 'test-put')">Test PUT</div>
		<div class="button" onclick="submitForm('testForm', 'test-head')">Test HEAD</div>
		<div class="button" onclick="submitForm('testForm', 'test-options')">Test OPTIONS</div>
		
		<div class="button" onclick="submitForm('testForm', 'test-url-get-content')">Test Url.getContent()</div>
		<div class="button" onclick="submitForm('testForm', 'test-put-no-response')">Test PUT without getInputStream</div>
	</div>
</div>

</body>
</html>
