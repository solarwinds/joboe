package com.tracelytics.test.struts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.opensymphony.xwork2.ActionSupport;
import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;

public class ReturnAction extends ActionSupport {

	public String execute() throws SAXException, IOException, ParserConfigurationException {
		testGetXMLString();
		testGetDocument();
		testGetDocumentBuilder();
		return SUCCESS;
	}
  
	@LogMethod(layer = "ReturnHugeString", storeReturn = true)
	@ProfileMethod(profileName = "ReturnProfile", storeReturn = true)
	public String testGetXMLString() throws FileNotFoundException {
		URL url = getClass().getResource("simple.xml");
		File file = new File(url.getPath());
		String XMLString = new Scanner(file).useDelimiter("\\A").next();
		return XMLString;
	}
	
	@LogMethod(layer = "ReturnDocument", storeReturn = true)
	@ProfileMethod(profileName = "ReturnProfile", storeReturn = true)
	public Document testGetDocument() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder(); 
		Document doc = db.parse(new File(getClass().getResource("simple.xml").getPath()));
		System.out.println(doc.getDocumentURI());
		return doc;
	}
	
	@LogMethod(layer = "ReturnWeirderThing", storeReturn = true)
	@ProfileMethod(profileName = "ReturnProfile", storeReturn = true)
	public DocumentBuilder testGetDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db;
	}
}
