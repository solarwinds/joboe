package com.tracelytics.test.struts;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.appoptics.api.ext.Trace;
import com.opensymphony.xwork2.ActionSupport;

public class SetTransactionNameAction extends ActionSupport {

	public String execute() throws SAXException, IOException, ParserConfigurationException {
	    Trace.setTransactionName("synchronous-set-transaction-name");
		return SUCCESS;
	}
}
