package com.appoptics.test;

import com.appoptics.test.model.User;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.*;

@Path("")
public class RootService {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessage() {
        return "This is root";
    }

}
