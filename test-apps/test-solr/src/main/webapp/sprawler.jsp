
<%-- <%@page import="com.tracelytics.api.RUMWrapper" %> --%>

<%@page import="java.util.UUID"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Solr sprawler test</title>
<link rel="stylesheet" type="text/css" href="css/justifiable/default.css"/>
<script src="scripts/jquery-2.0.3.min.js"></script>
<script>
	var elapsedTime = 0;
	var refreshIntervalInSecond = 5;
	var loadCollectedLinks = function(divId, sprawlerId)
	{
		$.getJSON("sprawlerPing.do?id=" + sprawlerId, function(data) {
			var items = [];
			
			var count = 0;
			$.each(data['collected'], function(key, value) {
				var liClass;
				if (value == 'PROCESSED') {
					liClass = "processed";
				} else if (value == 'QUERYING' || value == 'WAITING_FOR_CHILDREN') {
					liClass = "querying";
				} else if (value == 'INDEXING') {
					liClass = "indexing";
				} else if (value == 'OPENING') {
					liClass = "opening";
				} else if (value == 'ERROR') {
					liClass = "error";
				} else {
					liClass = "found";
				}
				items.push('<li class="' + liClass + '">' + $('<div/>').text(key).html() + '(' + value + ')</li>');
				count++;
			});
			
			
			if (data['isCompleted']) {
				if (data['collected']) {
					$(divId).html('Completed... Found ' + count);
				} else {
					$(divId).html('Completed...');
				}
				$('#startButton').attr("disabled", false);
				$('#startButton').attr("value", "Start");
			} else {
				$(divId).html('In Progress(refresh every ' + refreshIntervalInSecond + ' secs, grey indicates not yet processed entries)...Found ' + count);
				$('#startButton').attr("disabled", true);
				$('#startButton').attr("value", "Please wait...");
			}
			$(divId).append('<br/>Elapsed Time: ' + elapsedTime + "(s)");
			elapsedTime += refreshIntervalInSecond;
					
			if (data['collected']) {
			 $('<ol/>', {
				 'class': 'my-new-list',
				 html: items.join('')
				 }).appendTo(divId);
			}
			 if (!data['isCompleted']) {
				setTimeout("loadCollectedLinks('" + divId + "', '" + sprawlerId + "')", refreshIntervalInSecond * 1000); 
			 }
		});
	};

$(function() {
	$('#startButton').click(function(){
		$('p').text("Processing...");
		$('#startButton').attr("disabled", true);	
	});
});

</script>
</head>

<%
	UUID sprawlerId = (UUID)request.getAttribute("sprawlerId");
	String onLoadScript = sprawlerId != null ? ("onload=\"loadCollectedLinks('#status', '" + sprawlerId.toString() + "')\"") : "";
	String startingUrl = request.getParameter("startingUrl");
	if (startingUrl == null) {
	    startingUrl = "";
	}
	String depth = request.getParameter("depth");
	if (depth == null) {
	    depth = "1";
	}
	String collectionName = request.getParameter("collectionName");
	if (collectionName == null) {
		collectionName = "";
	}
%>


<body <%= onLoadScript %>>

<form action="sprawler.do">
Starting URL: <input type="text" name="startingUrl" maxlength="1000" size="50" value="<%= startingUrl %>"/> <br/>
Depth: <input type="text" name="depth" maxlength="2" size="5" value="<%= depth %>"/> <br/>
	Solr Collection URL (http://localhost:8080/solr-4, http://localhost:8983/solr/gettingstarted etc): <input type="text" name="solrServerUrl" value="<%= collectionName %>"/> <br/>
	<input type="submit" value="Start on Solr"/>
</form>
<div id="status">
	
</div>
</body>
</html>
