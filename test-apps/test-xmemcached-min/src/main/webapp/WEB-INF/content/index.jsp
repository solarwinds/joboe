<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>Spy memcached test</title>
</head>
<body>

<s:actionerror/>
<s:actionmessage/>
<div>
	<s:form action="test-all">
		<s:textfield name="key" label="Key" />
		<s:textfield name="durationString" label="Duration in second" value="60"/>
		<s:textfield name="value" label="Value" />
		<s:radio label="Connection Type" name="connectionType" list="#{'1' : 'binary', '2' : 'ascii'}" value="1"/>
		<s:submit value="Test All operations"/>
	</s:form>
</div>



</body>
</html>