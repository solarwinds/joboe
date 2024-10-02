package com.appoptics.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.appoptics.test.model.Message;
import com.appoptics.test.model.User;

@Path("/users/{username:[a-zA-Z_0-9]+}/messages")
public class UserMessagesService {
    private static final Map<User, List<Message>> ALL_MESSAGES = new HashMap<User, List<Message>>(); 
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Message> getMessages(@PathParam("username") String username) {
        List<Message> messages = getMessagesByUser(username);
        if (messages != null) {
            return Collections.unmodifiableList(messages);
        } else {
            return null;
        }
    }
    
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Message addMessage(@PathParam("username") String username, Message message) {
        List<Message> messages = getMessagesByUser(username);
        if (messages != null) {
            messages.add(message);
            return message;
        } else {
            return null;
        }
    }
    
    @DELETE
    @Path("/{messageIndex}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message deleteMessage(@PathParam("username") String username, @PathParam("messageIndex") int messageIndex) {
        List<Message> messages = getMessagesByUser(username);
        if (messages != null) {
            return messages.remove(messageIndex);
        } else {
            return null;
        }
    }
    
    @GET
    @Path("/{messageIndex}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message getMessage(@PathParam("username") String username, @PathParam("messageIndex") int messageIndex) {
        List<Message> messages = getMessagesByUser(username);
        if (messages != null) {
            return messages.get(messageIndex);
        } else {
            return null;
        }
    }
    
    
    private List<Message> getMessagesByUser(String username) {
        User user = UsersService.USERS.get(username);
        if (user == null) {
            return null;
        }
        if (ALL_MESSAGES.get(user) == null) {
            ALL_MESSAGES.put(user, new ArrayList<Message>());
        }
        return ALL_MESSAGES.get(user);
    }

}