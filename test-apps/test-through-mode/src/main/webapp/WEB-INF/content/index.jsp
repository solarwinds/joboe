<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="css/style.css"/>
<title>Through mode test</title>
</head>
<body>


<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	
	<div class="section">
		<div class="title">Trigger through trace on another (remote) server</div>
		<s:form action="test-through">
			Target URI: <s:textfield name="targetUri" cssStyle="width: 600px;"/>
			<s:submit value="query target url" cssClass="button"/>
		</s:form>
	</div>

</div>

</body>
</html>
