package com.appoptics.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.appoptics.test.model.User;

@Path("/users/{username:[a-zA-Z_0-9]+}")
public class UserService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("username") String username) {
        return UsersService.USERS.get(username);
    }
    
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public User deleteUser(@PathParam("username") String username) {
        return UsersService.USERS.remove(username);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public User putUser(User user) {
        if (user != null) {
            UsersService.USERS.put(user.getUsername(), user);
        }
        return user;
    }

}