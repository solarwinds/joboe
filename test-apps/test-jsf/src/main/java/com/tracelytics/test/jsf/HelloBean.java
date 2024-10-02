package com.tracelytics.test.jsf;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import java.io.Serializable;

@ManagedBean
@SessionScoped
public class HelloBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String name;

	public String getName() {
        new Exception().printStackTrace();
		return name;
	}

	public void setName(String name) {
        new Exception().printStackTrace();
		this.name = name;
	}
	
	public String getSayWelcome(){
        new Exception().printStackTrace();
		//check if null?
		if("".equals(name) || name ==null){
			return "";
		}else{
			return "Ajax message : Welcome " + name;
		}
	}
	
}
