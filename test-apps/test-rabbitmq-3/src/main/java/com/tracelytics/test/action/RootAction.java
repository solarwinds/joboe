package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Action;

import com.rabbitmq.client.Channel;

@SuppressWarnings("serial")

@Action("")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp"),
})
public class RootAction extends AbstractMqAction {
}
