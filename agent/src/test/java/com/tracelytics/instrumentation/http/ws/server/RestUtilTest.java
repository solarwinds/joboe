package com.tracelytics.instrumentation.http.ws.server;

import junit.framework.TestCase;

public class RestUtilTest extends TestCase{
    public void testFormatPath() {
        assertEquals("/", RestUtil.formatPath("/"));
        assertEquals("/users", RestUtil.formatPath("/users"));
        assertEquals("users", RestUtil.formatPath("users"));
        assertEquals("/:username", RestUtil.formatPath("/{username:[a-zA-Z][a-zA-Z_0-9]}"));
        assertEquals("/:username/:username2", RestUtil.formatPath("/{username}/{username2}"));
        assertEquals("/users/:username", RestUtil.formatPath("/users/{username:[a-zA-Z][a-zA-Z_0-9]}"));
        assertEquals("/users/:username/profile", RestUtil.formatPath("/users/{username:[a-zA-Z][a-zA-Z_0-9]}/profile"));
        assertEquals("/users/:username/profile", RestUtil.formatPath("/users/{username : [a-zA-Z][a-zA-Z_0-9]}/profile"));
        assertEquals("/users/:username/profile", RestUtil.formatPath("/users/{username : [a-zA-Z][a-zA-Z_0-9]{6}}/profile"));
        assertEquals("/users/:username/profile/:profileId/status", RestUtil.formatPath("/users/{username: [a-zA-Z][a-zA-Z_0-9]{6}}/profile/{profileId}/status"));
    }
}
