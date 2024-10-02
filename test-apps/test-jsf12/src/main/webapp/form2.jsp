<!DOCTYPE html
PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsf/core" %>
<%@ taglib prefix="h" uri="http://java.sun.com/jsf/html" %>
 
<f:view>
    <html>
    <head>
        <title>Form 2</title>
    </head>
    <body>
    <h:form>
        <p>Form 2</p>
        <p>Enter your message here: <br/>
        <h:inputText valueChangeListener="#{messageModel.printMessage}" value="#{message}" size="35"/></p>
        <h:commandButton value="Submit" action="#{messageModel.action2}" />
    </h:form>
    </body>
    </html>
</f:view>
