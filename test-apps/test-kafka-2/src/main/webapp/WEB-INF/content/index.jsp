<%@page import="com.tracelytics.test.WebsocketOutputServer"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@taglib uri="/struts-tags" prefix="s" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="../css/style.css"/>
<link rel="stylesheet" type="text/css" href="../css/jquery-ui.css"/>
<script src="../scripts/jquery-1.11.3.min.js"></script>
<script src="../scripts/jquery.form.min.js"></script>
<script src="../scripts/jquery-ui.min.js"></script>
<title>Kafka-2 test</title>
<script>

function submitForm(form, targetAction) {
    $(form).attr('action', targetAction);
	$(form).ajaxSubmit({
        dataType : 'json',
        success : function (response) {
        	if (response.responseMessage != null) {
	        	updateConsole('publish-console', response.responseMessage); //do not expect response from json anymore
	      	}
        }
    });
    $("html, body").animate({ scrollTop: 0 }, "slow");
}

function updateConsole(consoleId, message) {
	$('#' + consoleId).append('<p>' + message + '</p>');
	$('#' + consoleId).animate({ scrollTop: $('#' + consoleId)[0].scrollHeight}, "slow");
}

<% String baseWebsocketUrl = "ws://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/websocket-output"; %>

var wsPublishUri = "<%= baseWebsocketUrl %>/<%= session.getId() %>?<%= WebsocketOutputServer.SOCKET_ID_KEY %>=publish";
var wsConsumeUri = "<%= baseWebsocketUrl %>/<%= session.getId() %>?<%= WebsocketOutputServer.SOCKET_ID_KEY %>=consume";


$(document).ready(function() {
	websocket = new WebSocket(wsPublishUri);
	websocket.onmessage = function(evt) {
                    updateConsole('publish-console', evt.data);
                };
    websocket.onerror = function(evt) {
                    updateConsole('publish-console', 'Please ensure your app server supports websocket. For tomcat, it has to be 7.0.47+. ERROR: ' + evt.data);
                };
                
    websocket = new WebSocket(wsConsumeUri);
	websocket.onmessage = function(evt) {
                    updateConsole('consume-console', evt.data);
                };
    websocket.onerror = function(evt) {
                    updateConsole('consume-console', 'Please ensure your app server supports websocket. For tomcat, it has to be 7.0.47+. ERROR: ' + evt.data);
                };
                
                //initialize tooltip
                
                
                
   websocket = new WebSocket(wsDefaultUri);
   
   $(document).tooltip();
});

</script>
</head>
<body> 

<s:set var="consoleLineMax" value="15"/>

<div id="container" >
	<s:actionerror/>
	<s:actionmessage/>
	
	<div>Console for MQ client/publisher</div>
	<div class="console" id="publish-console"></div>

	<div>Console for MQ consumer</div>
	<div class="console" id="consume-console"></div>
	
	<div class="section">
		<form action="">
			<div class="title">Rabbit MQ host info</div>
			<div class="table">
				<div class="row">
					<div class="cell">Host</div>
					<div class="cell"><input type="text" name="mqForm.host" value="<s:property value='mqForm.host'/>"/></div>
					<div class="cell"><span class="ui-icon ui-icon-info" title="Host name of the Kafka zookeeper server" ></span></div>
				</div>
				<div class="row">
					<div class="cell">Port</div>
					<div class="cell"><input type="text" name="mqForm.port" value="<s:property value='mqForm.port'/>"/></div>
					<div class="cell"><span class="ui-icon ui-icon-info" title="Port number of the Kafka zookeeper port" ></span></div>
				</div>
				<div class="row">
						<div class="cell">Topic</div>
						<div class="cell"><input type="text" name="mqForm.topic" value="<s:property value='mqForm.topic'/>"/></div>
						<div class="cell"><span class="ui-icon ui-icon-info" title="Topic of the Kafka message" ></span></div>
					</div>
			</div>
			<div class="section">
				<div class="title">Add Kafka Consumer</div>
				<div class="row">
					<div class="cell">Consumer Group Id</div>
					<div class="cell"><input type="text" name="mqForm.consumerGroupId" value="<s:property value='mqForm.consumerGroupId'/>"/></div>
					<div class="cell"><span class="ui-icon ui-icon-info" title="Consumer Group Id to create" ></span></div>
				</div>
				
				<input type="button" value="Add Consumers" class="button" onclick="submitForm(this.form, 'add-consumers'); return false"/>
			</div>
			<div class="section">
                <div class="title">Add Kafka Streams</div>
				<input type="button" value="Add Streams" class="button" onclick="submitForm(this.form, 'add-streams'); return false"/>
				<div class="title">Add Kafka Streams that triggers test exception</div>
				<input type="button" value="Add Streams" class="button" onclick="submitForm(this.form, 'add-exception-streams'); return false"/>
			</div>



			<div class="section">
				<div class="title">Send Message to Kafka</div>
				<div class="table">
					<div class="row">
						<div class="cell">Message Key</div>
						<div class="cell"><input type="text" name="mqForm.messageKey" value="<s:property value='mqForm.messageKey'/>"/></div>
						<div class="cell"></div>
					</div>
					<div class="row">
						<div class="cell">Message</div>
						<div class="cell"><input type="text" name="mqForm.message" value="<s:property value='mqForm.message'/>"/></div>
						<div class="cell"></div>
					</div>
				</div>
				<div class="section">
					<div class="title">Public to topic</div>
					<input type="button" value="publish" class="button" onclick="submitForm(this.form, 'test-topic-publish'); return false"/>
				</div>
			</div>
		</form>
	</div>
	
	
</div>

</body>
</html>

