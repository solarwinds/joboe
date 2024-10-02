package com.tracelytics.test;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.Header;

import com.ebay.www.marketplace.search.v1.services.FindingServiceStub;
import com.ebay.www.marketplace.search.v1.services.FindingServiceStub.Amount;
import com.ebay.www.marketplace.search.v1.services.FindingServiceStub.FindItemsByKeywordsRequest;
import com.ebay.www.marketplace.search.v1.services.FindingServiceStub.FindItemsByKeywordsRequestE;
import com.ebay.www.marketplace.search.v1.services.FindingServiceStub.SearchItem;
import com.appoptics.api.ext.LogMethod;


/**
 * Really old impl using servlet using dispatcher!
 * @author Patson Luk
 *
 */
public class EbayServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        
        //WSDL2Java
        String keywords = req.getParameter("keywords");
        
        if (keywords == null) {
            req.getRequestDispatcher("ebay.jsp").forward(req, resp);
            return;
        }
        
        List<ItemInfo> result = testEbay(keywords);
        req.setAttribute("items", result);
        
        req.getRequestDispatcher("ebay.jsp").forward(req, resp);
    }

   
    
    @LogMethod(layer = "testEbay")
    private List<ItemInfo> testEbay(String keywords) throws RemoteException {
        FindingServiceStub stub = new FindingServiceStub();
        
//      Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
//      requestHeaders.put("X-EBAY-SOA-OPERATION-NAME", Collections.singletonList("findItemsByKeywords"));
//      requestHeaders.put("X-EBAY-SOA-SECURITY-APPNAME", Collections.singletonList("X-EBAY-SOA-SECURITY-APPNAME"));
      
        List<Header> headers = new ArrayList<Header>();
        headers.add(new Header("X-EBAY-SOA-OPERATION-NAME", "findItemsByKeywords"));
        headers.add(new Header("X-EBAY-SOA-SECURITY-APPNAME", "PatsonLu-96f8-4ba0-9420-db89aafa8129"));

        stub._getServiceClient().getOptions().setProperty(HTTPConstants.HTTP_HEADERS, headers);

        FindItemsByKeywordsRequestE request = new FindItemsByKeywordsRequestE();

        FindItemsByKeywordsRequest r = new FindItemsByKeywordsRequest();
        r.setKeywords(keywords);
        request.setFindItemsByKeywordsRequest(r);

        SearchItem[] itemsResult = stub.findItemsByKeywords(request).getFindItemsByKeywordsResponse().getSearchResult().getItem();
        
        List<ItemInfo> items = new ArrayList<ItemInfo>();
        for (SearchItem item : itemsResult) {
            items.add(new ItemInfo(item));
        }
        
        
        
        return items;
    }

    public static class ItemInfo {
        private String title;
        private String currencyId;
        private Double price;
        private String imageUrl;
        
        public String getTitle() {
            return title;
        }

        public String getCurrencyId() {
            return currencyId;
        }

        public Double getPrice() {
            return price;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        private ItemInfo(SearchItem item) {
            this.title = item.getTitle();
            if (item.getSellingStatus() != null && item.getSellingStatus().getCurrentPrice() != null) {
                Amount currentPrice = item.getSellingStatus().getCurrentPrice();
                if (currentPrice != null) {
                    currencyId = currentPrice.getCurrencyId();
                    this.price = currentPrice.get_double();
                }
            }
            
            this.imageUrl = item.getGalleryURL() != null ? item.getGalleryURL().toString() : null;
        }
        
        
        
    }
}