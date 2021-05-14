package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;



@WebFilter(urlPatterns={"/async-runnable-filter-servlet"}, asyncSupported=true)
public class AsyncRunnableFilter implements Filter {
   

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        final AsyncContext asyncContext= request.startAsync();
        
        response.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000); //sleep for 1 secs then complete the request
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    asyncContext.complete();
                }
            }
        });
        
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }
}
