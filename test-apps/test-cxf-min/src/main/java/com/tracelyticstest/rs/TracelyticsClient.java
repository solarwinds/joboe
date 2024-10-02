package com.tracelyticstest.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

public interface TracelyticsClient {
    @GET
    @Path("latency/{app}/server/summary")
    @Produces("application/json")
    Data getResult(@PathParam("app") String app, @QueryParam("key") String apiKey);
}
