package com.tracelytics.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket-output/{session-id}")
public class WebsocketOutputServer {
    private static final long serialVersionUID = 1L;
    public static final String SOCKET_ID_KEY = "socket-id";
    
    private static Map<String, Map<String, Session>> sessions = new HashMap<String, Map<String,Session>>();

    @OnOpen
    public void onOpen(Session session, @PathParam("session-id") String sessionId) {
        String socketId = session.getRequestParameterMap().containsKey(SOCKET_ID_KEY) ? session.getRequestParameterMap().get(SOCKET_ID_KEY).get(0) : null;
        
        System.out.println("Connected ... " + session.getId() + " server instance " + System.identityHashCode(this) + " session id " + sessionId + " socketId " + socketId);
        
        synchronized(sessions) {
            Map<String, Session> sessionsOfThisSessionId = sessions.get(sessionId);
            if (sessionsOfThisSessionId == null) {
                sessionsOfThisSessionId = new HashMap<String, Session>();
                sessions.put(sessionId, sessionsOfThisSessionId);
            }
            sessionsOfThisSessionId.put(socketId, session); //ok if no socket id is defined, use null as key (for a default websocket, probably the only one used in the session)
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println(String.format("Session %s closed because of %s", session.getId(), closeReason));
    }
    
    public static void outputMessage(String sessionId, String message) throws IOException {
        outputMessage(sessionId, null, message);
    }
    
    public static void outputMessage(String sessionId, String socketId, String message) throws IOException {
        Map<String, Session> sessionsOfThisSessionId = sessions.get(sessionId);
        if (sessionsOfThisSessionId == null) { //unexpected
            System.err.println("Cannot find websocket session for session id [" + sessionId + "]");
        } else { 
            Session session = sessionsOfThisSessionId.get(socketId);
            if (session == null) {
                System.err.println("Cannot find websocket session for session id [" + sessionId + "] with type [" + socketId + "]");
            } else {
                synchronized (session) {
                    session.getBasicRemote().sendText(message);
                }
            }
        }
    }
    
    static void removeOutputServer(String sessionId) {
        sessions.remove(sessionId);
    }

}
