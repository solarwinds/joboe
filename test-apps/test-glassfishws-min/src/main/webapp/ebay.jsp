<%@page import="com.tracelytics.test.EbayServlet.ItemInfo"%>
<%@page import="com.cdyne.ws.weatherws.Forecast"%>
<%@page import="java.util.List"%>

<%-- <%@page import="com.tracelytics.api.RUMWrapper" %> --%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ebay SOAP test</title>
<%-- <% out.println(RUMWrapper.getHeader()); %> --%>
<style type="text/css">
#box-table-a {
    border-collapse: collapse;
    font-family: "Lucida Sans Unicode","Lucida Grande",Sans-Serif;
    font-size: 12px;
    margin: 20px;
    text-align: left;
}
#box-table-a th {
    background: none repeat scroll 0 0 #B9C9FE;
    border-bottom: 1px solid #FFFFFF;
    border-top: 4px solid #AABCFE;
    color: #003399;
    font-size: 13px;
    font-weight: normal;
    padding: 8px;
}
#box-table-a td {
    background: none repeat scroll 0 0 #E8EDFF;
    border-bottom: 1px solid #FFFFFF;
    border-top: 1px solid transparent;
    color: #666699;
    padding: 8px;
}
#box-table-a tr:hover td {
    background: none repeat scroll 0 0 #D0DAFD;
    color: #333399;
}
</style>

</head>
<body>


<form action="ebay.do">
Search Keywords: 
	<input type="text" name="keywords" maxlength="60" size="40"/>
	<input type="submit" value="Do useless search on Ebay!"/>
</form>


<%
	List<ItemInfo> items = (List<ItemInfo>)request.getAttribute("items");

	if (items != null && !items.isEmpty()) {
%>
<table id="box-table-a">
	<thead>
		<tr>
			<th width="500px">Title</th>
			<th width="200px">Price</th>
			<th width="200px">PIC!</th>
		</tr>
	</thead>
	<tbody>
<%
		for (ItemInfo item : items) {
%>
	<tr>
		<td><%= item.getTitle() %></td>
		<td><%= item.getPrice() %>&nbsp;<%= item.getCurrencyId() %></td>
		<td>
		<% if (item.getImageUrl() != null) { %>
			<img src="<%= item.getImageUrl() %>"/>
		<% } %>
		</td>
	</tr>
<%
		}
%>
	</tbody>
</table>
<%
	} else {
%>
	--	
<%
	}
%>

</body>
</html>