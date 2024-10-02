/*
 * XML Type:  getInt
 * Namespace: http://com.sample
 * Java type: sample.com.GetInt
 *
 * Automatically generated - do not modify.
 */
package sample.com.impl;
/**
 * An XML getInt(@http://com.sample).
 *
 * This is a complex type.
 */
public class GetIntImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements sample.com.GetInt
{
    
    public GetIntImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName ARG0$0 = 
        new javax.xml.namespace.QName("", "arg0");
    
    
    /**
     * Gets the "arg0" element
     */
    public int getArg0()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(ARG0$0, 0);
            if (target == null)
            {
                return 0;
            }
            return target.getIntValue();
        }
    }
    
    /**
     * Gets (as xml) the "arg0" element
     */
    public org.apache.xmlbeans.XmlInt xgetArg0()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlInt target = null;
            target = (org.apache.xmlbeans.XmlInt)get_store().find_element_user(ARG0$0, 0);
            return target;
        }
    }
    
    /**
     * Sets the "arg0" element
     */
    public void setArg0(int arg0)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(ARG0$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(ARG0$0);
            }
            target.setIntValue(arg0);
        }
    }
    
    /**
     * Sets (as xml) the "arg0" element
     */
    public void xsetArg0(org.apache.xmlbeans.XmlInt arg0)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlInt target = null;
            target = (org.apache.xmlbeans.XmlInt)get_store().find_element_user(ARG0$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlInt)get_store().add_element_user(ARG0$0);
            }
            target.set(arg0);
        }
    }
}
