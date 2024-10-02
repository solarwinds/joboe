//package com.tracelytics.test.spring.config;
//
//import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
//
//public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer  {
//
//        @Override
//        protected Class<?>[] getRootConfigClasses() {
//                return new Class[] { Config.class };
//        }
//
//        @Override
//        protected Class<?>[] getServletConfigClasses() {
//                return null;
//        }
//
//        @Override
//        protected String[] getServletMappings() {
//                return new String[] { "/" };
//        }
//}

package com.tracelytics.test.spring.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WebInitializer implements WebApplicationInitializer {

        public void onStartup(ServletContext servletContext) throws ServletException {

                AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
                ctx.register(Config.class);
                ctx.setServletContext(servletContext);

                Dynamic servlet = servletContext.addServlet("dispatcher", new DispatcherServlet(ctx));
                servlet.addMapping("/");
                servlet.setLoadOnStartup(1);
                servlet.setAsyncSupported(true);

        }

}