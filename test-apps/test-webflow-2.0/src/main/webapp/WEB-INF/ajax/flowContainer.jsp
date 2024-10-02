<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Ajax Container</title>
	<script type="text/javascript" src="<c:url value="/resources/dojo/dojo.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/spring/Spring.js" />"></script>
    <script type="text/javascript" src="<c:url value="/resources/spring/Spring-Dojo.js" />"></script>
    <link rel="stylesheet" href="<c:url value="/resources/styles/style.css" />" type="text/css" media="screen" />
</head>
<body>
	<div id="container">
		<div class="label">Embedded Flow Container Page</div>
		<hr>
		<div style="display: inline-block; float: left; width: 20%">
			<h6>Some text to the left</h6>
			<p>Some text to the left</p>
		</div>
		<div style="display: inline-block; float: left; width: 60%">
			<h3 class="alt">Embedded Flow Area</h3>
			<div id="embeddedFlow">
				<a id="startFlow" href="test">Start Embedded Flow</a>
				<script type="text/javascript">
					Spring.addDecoration(new Spring.AjaxEventDecoration({elementId:"startFlow",event:"onclick",params:{fragments:"body",mode:"embedded"}}));
				</script>
			</div>
		</div>
		<div style="display: inline-block; float: right; width: 20%">
			<h6>Some text to the right</h6>
			<p>Some text to the right</p>
		</div>
		
		<div style="display: none; clear: both;"></div>
	</div>
</body>
</html>