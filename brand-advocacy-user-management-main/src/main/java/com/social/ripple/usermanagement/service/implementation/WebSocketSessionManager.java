package com.social.ripple.usermanagement.service.implementation;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public class WebSocketSessionManager {

    private static final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public static void addSession(String sessionId, WebSocketSession session) {
        activeSessions.put(sessionId, session);
    }

    public static WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public static void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public static boolean isSessionActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    public static int getActiveSessionsCount() {
        return activeSessions.size();
    }
}