
package sample.com;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the sample.com package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetIntResponse_QNAME = new QName("http://com.sample", "getIntResponse");
    private final static QName _GetInt_QNAME = new QName("http://com.sample", "getInt");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sample.com
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetIntResponse }
     * 
     */
    public GetIntResponse createGetIntResponse() {
        return new GetIntResponse();
    }

    /**
     * Create an instance of {@link GetInt }
     * 
     */
    public GetInt createGetInt() {
        return new GetInt();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetIntResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://com.sample", name = "getIntResponse")
    public JAXBElement<GetIntResponse> createGetIntResponse(GetIntResponse value) {
        return new JAXBElement<GetIntResponse>(_GetIntResponse_QNAME, GetIntResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetInt }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://com.sample", name = "getInt")
    public JAXBElement<GetInt> createGetInt(GetInt value) {
        return new JAXBElement<GetInt>(_GetInt_QNAME, GetInt.class, null, value);
    }

}
