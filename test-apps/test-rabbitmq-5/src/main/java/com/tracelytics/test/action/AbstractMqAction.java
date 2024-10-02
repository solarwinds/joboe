package com.tracelytics.test.action;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;
import org.apache.struts2.interceptor.SessionAware;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public abstract class AbstractMqAction extends ActionSupport implements Preparable, SessionAware {
    private List<String> extendedOutput;


    protected Map<String, Object> session;
    protected MqForm mqForm;

    protected static final String EXCHANGE_DIRECT = "test-exchange-direct";
    protected static final String EXCHANGE_FANOUT = "test-exchange-fanout";
    protected static final String EXCHANGE_TOPIC = "test-exchange-topic";
    
    public MqForm getMqForm() {
        return mqForm ;
    }
    
    @Override
    public void prepare() throws Exception {
        extendedOutput = null; //clear the output
        if (!session.containsKey("mqForm")) {
            session.put("mqForm", new MqForm());
        }
        
        mqForm = (MqForm) session.get("mqForm");
    }
    
    public List<String> getExtendedOutput() {
        return extendedOutput;
    }
    
    

    public void setExtendedOutput(List<String> extendedOutput) {
        this.extendedOutput = extendedOutput;
    }

    public void appendExtendedOutput(String text) {
        if (extendedOutput == null) {
            extendedOutput = new LinkedList<String>();
        }
        extendedOutput.add(text);
    }

   
    protected void printToOutput(String title, Map<?, ?> map) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : map.entrySet()) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, List<?> keys) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : keys) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, Object...items) {
        printToOutput(title, Arrays.asList(items));
    }

    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }
}
