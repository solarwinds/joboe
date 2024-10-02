package com.tracelyticstest.rs;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

public interface SampleServerClient {
    @GET
    @Path("something/{param1}")
    @Produces("application/json")
    SampleResult getResult(@PathParam("param1") String param1, @QueryParam("parma2") String param2);
    
    @POST
    @Path("something/{param1}")
    @Produces("application/json")
    SampleResult postResult(@PathParam("param1") String param1, @QueryParam("parma2") String param2);
    
    @DELETE
    @Path("something/{param1}")
    @Produces("application/json")
    SampleResult deleteResult(@PathParam("param1") String param1, @QueryParam("parma2") String param2);
    
    @PUT
    @Path("something/{param1}")
    @Produces("application/json")
    SampleResult putResult(@PathParam("param1") String param1, @QueryParam("parma2") String param2);
    
    @GET
    @Path("wait")
    SampleResult getWait(@QueryParam("duration") long duration);
    
}
