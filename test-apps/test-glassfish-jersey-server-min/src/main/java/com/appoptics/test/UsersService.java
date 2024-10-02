package com.appoptics.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.appoptics.test.model.User;

@Path("/users")
public class UsersService {
    static final Map<String, User> USERS = new LinkedHashMap<String, User>();
    static {
        USERS.put("patson", new User("patson"));
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> getUser() {
        return Collections.unmodifiableList(new ArrayList<User>(USERS.values()));
    }

}
