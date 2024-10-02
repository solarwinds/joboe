/*
 * An XML document type.
 * Localname: getIntResponse
 * Namespace: http://com.sample
 * Java type: sample.com.GetIntResponseDocument
 *
 * Automatically generated - do not modify.
 */
package sample.com.impl;
/**
 * A document containing one getIntResponse(@http://com.sample) element.
 *
 * This is a complex type.
 */
public class GetIntResponseDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements sample.com.GetIntResponseDocument
{
    
    public GetIntResponseDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName GETINTRESPONSE$0 = 
        new javax.xml.namespace.QName("http://com.sample", "getIntResponse");
    
    
    /**
     * Gets the "getIntResponse" element
     */
    public sample.com.GetIntResponse getGetIntResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetIntResponse target = null;
            target = (sample.com.GetIntResponse)get_store().find_element_user(GETINTRESPONSE$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Sets the "getIntResponse" element
     */
    public void setGetIntResponse(sample.com.GetIntResponse getIntResponse)
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetIntResponse target = null;
            target = (sample.com.GetIntResponse)get_store().find_element_user(GETINTRESPONSE$0, 0);
            if (target == null)
            {
                target = (sample.com.GetIntResponse)get_store().add_element_user(GETINTRESPONSE$0);
            }
            target.set(getIntResponse);
        }
    }
    
    /**
     * Appends and returns a new empty "getIntResponse" element
     */
    public sample.com.GetIntResponse addNewGetIntResponse()
    {
        synchronized (monitor())
        {
            check_orphaned();
            sample.com.GetIntResponse target = null;
            target = (sample.com.GetIntResponse)get_store().add_element_user(GETINTRESPONSE$0);
            return target;
        }
    }
}
