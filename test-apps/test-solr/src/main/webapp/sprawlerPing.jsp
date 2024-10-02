
<%@page import="java.util.Set"%>
<%
	Set<String> collected = (Set<String>)request.getAttribute("collected");
	Boolean isCompleted = (Boolean)request.getAttribute("isCompleted");
	
	if (isCompleted != null && isCompleted) {
	    
%>
Completed
<% 
	} else {
%>
In Progress
<% } %>


