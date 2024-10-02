
<%-- <%@page import="com.tracelytics.api.RUMWrapper" %> --%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Solr data import test</title>
</head>
<body>

<form action="test.do">
	<input type="hidden" name="solrServerUrl" id="solrServerUrl"/>
Document Id: <input type="text" name="id" maxlength="20" size="10"/> <br/>
Document name: <input type="text" name="name" maxlength="50" size="20"/> <br/>
	Solr 5+ path: <input type="text" name="path" maxlength="50" size="20" value="solr/gettingstarted"/> <br/>
	<input type="submit" value="Submit to Solr 3" onclick="document.getElementById('solrServerUrl').value = 'http://localhost:8080/solr-3/'"/>
	<input type="submit" value="Submit to Solr 4" onclick="document.getElementById('solrServerUrl').value = 'http://localhost:8080/solr-4/'"/>
	<input type="submit" value="Submit to Solr 5+" onclick="document.getElementById('solrServerUrl').value = 'http://localhost:8983/'"/>
</form>

<%
	Boolean success = (Boolean)request.getAttribute("success");
	if (success != null) {
	    if (success) {
%>
	<div>	
		document indexed!
	</div>
	
<%
		} else {
%>
	<div>	
		document indexing failed!
	</div>
<%
		}
	}
%>
</body>
</html>