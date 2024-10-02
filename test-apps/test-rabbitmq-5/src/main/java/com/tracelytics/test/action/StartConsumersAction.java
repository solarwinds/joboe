package com.tracelytics.test.action;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.*;
import com.tracelytics.test.WebsocketOutputServer;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")

@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class StartConsumersAction extends AbstractMqAction {
    private static Set<String> initializedHosts = new HashSet<String>();
    
    @Override
    public String execute() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqForm.getHost());
        factory.setPort(mqForm.getPort());
        Connection connection = factory.newConnection();
        
        final Channel channel = connection.createChannel(); 
        channel.exchangeDeclare(EXCHANGE_DIRECT, "direct");
        channel.exchangeDeclare(EXCHANGE_FANOUT, "fanout");
        channel.exchangeDeclare(EXCHANGE_TOPIC, "topic");
        channel.basicQos(1);        
        
        String sessionId = ServletActionContext.getRequest().getSession().getId();
        synchronized(initializedHosts) {
            if (!initializedHosts.contains(sessionId)) {
                //declare queues and bind consumers to direct exchange
                bindConsumer(connection.createChannel(), EXCHANGE_DIRECT, "wait", new WaitFunction(sessionId)); 
                bindConsumer(connection.createChannel(), EXCHANGE_DIRECT, "echo", new EchoFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_DIRECT, "async_wait", new AsyncWaitFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_DIRECT, "nop", new NopFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_DIRECT, "error", new ErrorFunction(sessionId));
                
                //declare queues and bind consumers to fanout exchange
                ConsumingFunction echoFunction = new EchoFunction(sessionId);
                bindConsumer(connection.createChannel(), EXCHANGE_FANOUT, "", echoFunction);
                bindConsumer(connection.createChannel(), EXCHANGE_FANOUT, "", echoFunction);
                bindConsumer(connection.createChannel(), EXCHANGE_FANOUT, "", echoFunction);
                
                //declare queues and bind consumers to topic exchange
                bindConsumer(connection.createChannel(), EXCHANGE_TOPIC, "*.wait", new WaitFunction(sessionId)); 
                bindConsumer(connection.createChannel(), EXCHANGE_TOPIC, "*.echo", new EchoFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_TOPIC, "*.async_wait", new AsyncWaitFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_TOPIC, "*.nop", new NopFunction(sessionId));
                bindConsumer(connection.createChannel(), EXCHANGE_TOPIC, "*.error", new ErrorFunction(sessionId));
                
                initializedHosts.add(sessionId);
                WebsocketWriter.write("consume", "Added consumers session id [" + sessionId + "]");
            } else {
                WebsocketOutputServer.outputMessage(sessionId, "consume", "Consumers already exist for session id [" + sessionId + "]");
            }
        }
       
        
        return SUCCESS;
    }
    
    
    
    private void bindConsumer(final Channel channel, final String exchange, String bindingKey, final ConsumingFunction consumingFunction) throws IOException {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-max-length", 2);
        String queueName = channel.queueDeclare("", true, true, true, args).getQueue();
        channel.queueBind(queueName, exchange, bindingKey);

        channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
                String result = consumingFunction.consume(body);

                if (properties != null && properties.getReplyTo() != null) {
                    com.rabbitmq.client.AMQP.BasicProperties replyProps = new BasicProperties
                            .Builder()
                            .correlationId(properties.getCorrelationId())
                            .build();
                    channel.basicPublish( "", properties.getReplyTo(), replyProps, result.getBytes());
                }

                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        });
    }
    
    private interface ConsumingFunction {
        String consume(byte[] body);
    }
    
    private abstract class ConsumingFunctionWithWebsocketOutput implements ConsumingFunction{
        private String sessionId;
        public ConsumingFunctionWithWebsocketOutput(String sessionId) {
            this.sessionId = sessionId;
        }
        
        protected void output(String message) {
            try {
                WebsocketOutputServer.outputMessage(sessionId, "consume", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class WaitFunction extends ConsumingFunctionWithWebsocketOutput {
        public WaitFunction(String sessionId) {
            super(sessionId);
        }

        @Override
        public String consume(byte[] body) {
            String message = new String(body);
            try {
                int duration = Integer.parseInt(message);
                output("Recieved message [" + message + "]...waiting for " + duration + "ms. This is blocking"); 
                Thread.sleep(duration);
                output("Finished waiting for " + duration + "ms");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return message;
        }
    }
    
    private class AsyncWaitFunction extends ConsumingFunctionWithWebsocketOutput {
        public AsyncWaitFunction(String sessionId) {
            super(sessionId);
        }

        @Override
        public String consume(byte[] body) {
            final String message = new String(body);
            new Thread() {
                @Override
                public void run() {
                    try {
                        int duration = Integer.parseInt(message);
                        output("Recieved message [" + message + "]...waiting for " + duration + "ms. This is non-blocking"); 
                        Thread.sleep(duration);
                        output("Finished waiting for " + duration + "ms");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }.start();
            
            return message;
        }
    }
    
    
    private class EchoFunction extends ConsumingFunctionWithWebsocketOutput {
        public EchoFunction(String sessionId) {
            super(sessionId);
        }

        @Override
        public String consume(byte[] body) {
            String message = new String(body);
            output("Recieved message [" + message + "]...just echoing it..."); 

            return message;
        }
        
    }
    
    private class NopFunction extends ConsumingFunctionWithWebsocketOutput {
        public NopFunction(String sessionId) {
            super(sessionId);
        }

        @Override
        public String consume(byte[] body) {
            return null;
        }
        
    }

    private class ErrorFunction extends ConsumingFunctionWithWebsocketOutput {
        public ErrorFunction(String sessionId) {
            super(sessionId);
        }

        @Override
        public String consume(byte[] body) {
            throw new RuntimeException("Test exception");
        }

    }
    

}
