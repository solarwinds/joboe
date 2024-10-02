package hello;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.ProducerCallback;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.jms.*;

@Controller
public class ProducerController {
    @GetMapping("/produce")
    public String produce(@RequestHeader MultiValueMap<String, String> headers, Model model) {
        JmsTemplate jmsTemplate = Application.context.getBean(JmsTemplate.class);
        // Send a message with a POJO - the template reuse the message converter
        System.out.println("Sending an email message.");
        jmsTemplate.convertAndSend("mailbox", new Email("info@example.com", "Hello"));

        jmsTemplate.execute(new ProducerCallback<Object>() {
                                @Override
                                public Object doInJms(Session session, MessageProducer messageProducer) throws JMSException {
                                    messageProducer.send(jmsTemplate.getDestinationResolver().resolveDestinationName(session, "mailbox", false),
                                            jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send1"), session)
                                    );
                                    messageProducer.send(jmsTemplate.getDestinationResolver().resolveDestinationName(session, "mailbox", false),
                                            jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send2"), session),
                                            new CompletionListener() {
                                                @Override
                                                public void onCompletion(Message message) {
                                                    // do nothing
                                                }

                                                @Override
                                                public void onException(Message message, Exception exception) {
                                                    // do nothing
                                                }
                                            }
                                    );
                                    messageProducer.send(jmsTemplate.getDestinationResolver().resolveDestinationName(session, "mailbox", false),
                                            jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send3"), session),
                                            1,
                                            1,
                                            1000
                                    );
                                    messageProducer.send(jmsTemplate.getDestinationResolver().resolveDestinationName(session, "mailbox", false),
                                            jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send4"), session),
                                            1,
                                            1,
                                            1000,
                                            new CompletionListener() {
                                                @Override
                                                public void onCompletion(Message message) {
                                                    // do nothing
                                                }

                                                @Override
                                                public void onException(Message message, Exception exception) {
                                                    // do nothing
                                                }
                                            }
                                    );
                                    return null;
                                }
                            }
        );
        // with a default destination
        jmsTemplate.execute("junk", new ProducerCallback<Object>() {
            @Override
            public Object doInJms(Session session, MessageProducer messageProducer) throws JMSException {
                messageProducer.send(jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send5"), session)
                );
                messageProducer.send(jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send6"), session),
                        new CompletionListener() {
                            @Override
                            public void onCompletion(Message message) {
                                // do nothing
                            }

                            @Override
                            public void onException(Message message, Exception exception) {
                                // do nothing
                            }
                        }
                );
                messageProducer.send(jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send7"), session),
                        1,
                        1,
                        1000
                );
                messageProducer.send(jmsTemplate.getMessageConverter().toMessage(new Email("hello@test.com", "Hello send8"), session),
                        1,
                        1,
                        1000,
                        new CompletionListener() {
                            @Override
                            public void onCompletion(Message message) {
                                // do nothing
                            }

                            @Override
                            public void onException(Message message, Exception exception) {
                                // do nothing
                            }
                        }
                );
                return null;
            }
        });
        return "greeting";
    }
    }

