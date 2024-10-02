package com.tracelytics.test;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

public interface SampleClient {
    @GET
    @Path("something/{param}")
    @Produces("application/json")
    SampleResultContainer getResult(@PathParam("param") String param, @QueryParam("duration") int duration);
    
    @DELETE
    @Path("something/{param}")
    @Produces("application/json")
    SampleResultContainer deleteResult(@PathParam("param") String param, @QueryParam("duration") int duration);

    @PUT
    @Path("something/{param}")
    @Produces("application/json")
    SampleResultContainer putResult(@PathParam("param") String param, @QueryParam("duration") int duration);
    
    @POST
    @Path("something/{param}")
    @Produces("application/json")
    SampleResultContainer postResult(@PathParam("param") String param, @QueryParam("duration") int duration);
}


