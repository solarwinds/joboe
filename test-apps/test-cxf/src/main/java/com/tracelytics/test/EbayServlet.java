package com.tracelytics.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import com.ebay.marketplace.search.v1.services.Amount;
import com.ebay.marketplace.search.v1.services.FindItemsByKeywordsRequest;
import com.ebay.marketplace.search.v1.services.FindItemsByKeywordsResponse;
import com.ebay.marketplace.search.v1.services.FindingService;
import com.ebay.marketplace.search.v1.services.FindingServicePortType;
import com.ebay.marketplace.search.v1.services.SearchItem;
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
    private List<ItemInfo> testEbay(String keywords) throws MalformedURLException {
        FindingServicePortType servicePort = new FindingService(FindingService.class.getClassLoader().getResource("wsdl/FindingService.wsdl")).getFindingServiceSOAPPort();
        
        Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
        
        requestHeaders.put("X-EBAY-SOA-OPERATION-NAME", Collections.singletonList("findItemsByKeywords"));
        requestHeaders.put("X-EBAY-SOA-SECURITY-APPNAME", Collections.singletonList("PatsonLu-96f8-4ba0-9420-db89aafa8129"));
        
        ((BindingProvider)servicePort).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
        
        FindItemsByKeywordsRequest request = new FindItemsByKeywordsRequest();
        request.setKeywords(keywords);
        
        FindItemsByKeywordsResponse response = servicePort.findItemsByKeywords(request);
        
        List<ItemInfo> items = new ArrayList<ItemInfo>();
        for (SearchItem item : response.getSearchResult().getItem()) {
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
                    this.price = currentPrice.getValue();
                }
            }
            
            this.imageUrl = item.getGalleryURL();
        }
        
        
        
    }
}

