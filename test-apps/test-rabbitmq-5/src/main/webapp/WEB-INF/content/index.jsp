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
<title>RabbitMQ test</title>
<script>

function submitForm(submitButton, targetAction) {
	var form = submitButton.form
    $(form).attr('action', targetAction);
	var routingKeyInput = $(form).find("input[name=\"mqForm.routingKey\"]");
	if ($(submitButton).closest(".publish").length) {
		routingKeyInput.val($(submitButton).closest(".publish").find(".routingKey").val())
	} else if ($(submitButton).closest(".basic-get").length) {
		routingKeyInput.val($(submitButton).closest(".basic-get").find(".routingKey").val())
	}

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
					<div class="cell"><span class="ui-icon ui-icon-info" title="Host name of the RabbitMQ server" ></span></div>
				</div>
				<div class="row">
					<div class="cell">Port</div>
					<div class="cell"><input type="text" name="mqForm.port" value="<s:property value='mqForm.port'/>"/></div>
					<div class="cell"><span class="ui-icon ui-icon-info" title="Port number of the RabbitMQ server, use empty for default port" ></span></div>
				</div>
			</div>
			<div class="section">
				<div class="title">Start Rabbit MQ Consumers</div>
				<input type="button" value="Start Consumers" class="button" onclick="submitForm(this, 'start-consumers'); return false"/>
			</div>
			<div class="section publish">
				<div class="title">Send Rabbit MQ Message</div>
				<div class="table">
					<div class="row">
						<div class="cell">Routing Key</div>
						<div class="cell"><input type="text" class="routingKey"></div>
						<div class="cell"><span class="ui-icon ui-icon-info" title="Routing Key for topic and direct exchange. For direct exchange, valid routing keys are 'wait', 'async_wait', 'echo', 'error' or 'nop'. For topic exchange, valid routing keys patterns are '*.wait', '*.async_wait', '*.echo', '*.error' and '*.nop' (for example 'something.wait'). For fanout exchange, routing key is ignored" ></span></div>
					</div>
					<div class="row">
						<div class="cell">Message</div>
						<div class="cell"><input type="text" name="mqForm.message" value="<s:property value='mqForm.message'/>"/></div>
						<div class="cell"><span class="ui-icon ui-icon-info" title="Message for the exchange. For 'wait' and 'async_wait' routing key, use integer string here to control the wait duration in millisec" ></span></div>
					</div>
				</div>
				<div class="section">
					<div class="title">To direct exchange</div>
					<input type="button" value="publish" class="button" onclick="submitForm(this, 'test-direct-publish'); return false"/>
		<%-- 			<input type="button" value="Send Message as Publish (Transaction mode)" class="button" onclick="submitForm(this.form, 'test-direct-publish-transaction-mode'); return false"/> --%>
					<input type="button" value="publish (Mandatory)" class="button" onclick="submitForm(this, 'test-direct-mandatory-publish'); return false"/>
					<input type="button" value="rpc call using base classes" class="button" onclick="submitForm(this, 'test-direct-rpc'); return false"/>
					<input type="button" value="rpc call using rpc client" class="button" onclick="submitForm(this, 'test-rpc-client'); return false"/>
				</div>
				<div class="section">
					<div class="title">To topic exchange</div>
					<input type="button" value="publish" class="button" onclick="submitForm(this, 'test-topic-publish'); return false"/>
				</div>	
				<div class="section">
					<div class="title">To fanout exchange</div>
					<input type="button" value="publish" class="button" onclick="submitForm(this, 'test-fanout-publish'); return false"/>
				</div>
			</div>
			<div class="section basic-get">
				<div class="title">Basic Get Operations</div>
				<div class="table">
					<div class="row">
						<div class="cell">Routing Key</div>
						<div class="cell"><input type="text" class="routingKey"></div>
						<div class="cell"><span class="ui-icon ui-icon-info" title="Routing Key to listen to."></span></div>
					</div>
				</div>
				<div class="section">
					<div class="title">Bind queue to direct exhange</div>
					<input type="button" value="Bind Queue" class="button" onclick="submitForm(this, 'start-basic-get-queue'); return false"/>
				</div>
				<div class="section">
					<div class="title">Basic get Message from direct exchange queue</div>
					<input type="button" value="Basic Get" class="button" onclick="submitForm(this, 'test-basic-get'); return false"/>
				</div>
				<div class="section">
					<div class="title">Basic get Message from direct exchange queue (clear context before get action)</div>
					<input type="button" value="Basic Standalone Get" class="button" onclick="submitForm(this, 'test-standalone-get'); return false"/>
				</div>
			</div>
			<input type="hidden" name="mqForm.routingKey">
		</form>
	</div>
	
	
</div>

</body>
</html>

