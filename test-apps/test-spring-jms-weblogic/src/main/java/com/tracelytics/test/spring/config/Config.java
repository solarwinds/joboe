package com.tracelytics.test.spring.config;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import com.tracelytics.test.spring.queue.QueueConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"com.tracelytics.test.spring"})
public class Config  {
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

//    @Bean
//    public InternalResourceViewResolver jspResolver() {
//        InternalResourceViewResolver resourceResolver = new InternalResourceViewResolver();
//        resourceResolver.setPrefix("/WEB-INF/jsps/");
//        resourceResolver.setSuffix(".jsp");
//        return resourceResolver;
//    }
    @Bean
    public UrlBasedViewResolver setupViewResolver() {
        UrlBasedViewResolver resolver = new UrlBasedViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        resolver.setViewClass(JstlView.class);
        return resolver;
    }

    @Bean
    public JndiTemplate jndiTemplate() {
        return new JndiTemplate();
    }

    @Bean
    public JndiObjectFactoryBean queueConnectionFactory() {
        JndiObjectFactoryBean queueConnectionFactory = new JndiObjectFactoryBean();
        queueConnectionFactory.setJndiTemplate(jndiTemplate());
        queueConnectionFactory.setJndiName("jms/TestConnectionFactory");
        return queueConnectionFactory;
    }

    @Bean
    public JndiDestinationResolver jmsDestinationResolver() {
        JndiDestinationResolver destResolver = new JndiDestinationResolver();
        destResolver.setJndiTemplate(jndiTemplate());
        destResolver.setCache(true);

        return destResolver;
    }

    @Bean
    public JmsTemplate queueSenderTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory((ConnectionFactory) queueConnectionFactory().getObject());
        template.setDestinationResolver(jmsDestinationResolver());
        return template;
    }

    @Bean
    public JndiObjectFactoryBean jmsQueue() {
        JndiObjectFactoryBean jmsQueue = new JndiObjectFactoryBean();
        jmsQueue.setJndiTemplate(jndiTemplate());
        jmsQueue.setJndiName("jms/TestJMSQueue");

        return jmsQueue;
    }

//    @Bean
//    public QueueConsumer queueListener() {
//        return new QueueConsumer();
//    }
//
//    @Bean
//    public DefaultMessageListenerContainer messageListener() {
//        DefaultMessageListenerContainer listener = new DefaultMessageListenerContainer();
//        listener.setConcurrentConsumers(5);
//        listener.setConnectionFactory((ConnectionFactory) queueConnectionFactory().getObject());
//        listener.setDestination((Destination) jmsQueue().getObject());
//        listener.setMessageListener(queueListener());
//
//        return listener;
//    }
}