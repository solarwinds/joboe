package com.tracelytics.test.action;

import java.util.Random;

import org.hibernate.Session;

import com.opensymphony.xwork2.ActionSupport;
import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.ProfileMethod;
import com.tracelytics.test.hibernate.HibernateUtil;
import com.tracelytics.test.hibernate.model.Child;
import com.tracelytics.test.hibernate.model.Parent;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
                                                   @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
                                                   @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
})
public class TestHibernate extends ActionSupport {
    @Override
    public String execute() throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();

        int parentId = testSave(session);
        session.close();
        
        session = HibernateUtil.getSessionFactory().openSession();
        Parent loadedParent = loadParent(session, parentId);
        updateChild(session, loadedParent);
        session.close();
        
        addActionMessage("Hibernate operations performed successfully");
        
        return SUCCESS;
    }

    @ProfileMethod(profileName="load_parent")
    private Parent loadParent(Session session, int parentId) {
        return (Parent)session.get(Parent.class, parentId);
    }
    
    @ProfileMethod(profileName="update_child_save_parent")
    private void updateChild(Session session, Parent parent) {
        session.beginTransaction();
        
        parent.getChild().getName();
        parent.getChild().setName(String.valueOf(new Random().nextInt()));

        session.saveOrUpdate(parent);
        
        session.getTransaction().commit();
    }    

    @ProfileMethod(profileName="save_parent_and_child")
    private int testSave(Session session) {
        session.beginTransaction();
        Parent parent = new Parent();
        parent.setName("Hibernate parent");
        
        Child child = new Child();
        child.setName("Hibernate child");
        
        parent.setChild(child);
        
        session.saveOrUpdate(parent);
        
        int generatedId = parent.getId();
        
        session.getTransaction().commit();
        
        return generatedId;
    }
}
