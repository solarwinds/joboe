/*
 * An XML document type.
 * Localname: getInt
 * Namespace: http://com.sample
 * Java type: sample.com.GetIntDocument
 *
 * Automatically generated - do not modify.
 */
package sample.com.impl;
/**
 * A document containing one getInt(@http://com.sample) element.
 *
 * This is a complex type.
 */
public class GetIntDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements sample.com.GetIntDocument
{
    
    public GetIntDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETINT$0 = 
        new javax.xml.namespace.QName("http://com.sample", "getInt");
    
    
    /**
     * Gets the "getInt" element
     */
    public sample.com.GetInt getGetInt()
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetInt target = null;
            target = (sample.com.GetInt)get_store().find_element_user(GETINT$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "getInt" element
     */
    public void setGetInt(sample.com.GetInt getInt)
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetInt target = null;
            target = (sample.com.GetInt)get_store().find_element_user(GETINT$0, 0);
            if (target == null)
            {
                target = (sample.com.GetInt)get_store().add_element_user(GETINT$0);
            }
            target.set(getInt);
        }
    }
    
    /**
     * Appends and returns a new empty "getInt" element
     */
    public sample.com.GetInt addNewGetInt()
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetInt target = null;
            target = (sample.com.GetInt)get_store().add_element_user(GETINT$0);
            return target;
        }
    }
}
