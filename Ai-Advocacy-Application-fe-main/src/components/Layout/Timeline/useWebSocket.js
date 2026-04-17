// useWebSocket.js
import { useEffect, useRef } from "react";

const useWebSocket = (url, onMessage) => {
  const ws = useRef(null);
  const heartbeatRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const onMessageRef = useRef(onMessage);
  const manuallyClosedRef = useRef(false);
  const reconnectAttemptRef = useRef(0);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!url) return;

    let isMounted = true;
    manuallyClosedRef.current = false;
    reconnectAttemptRef.current = 0;

    const clearHeartbeat = () => {
      clearInterval(heartbeatRef.current);
      heartbeatRef.current = null;
    };

    const clearReconnectTimer = () => {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    };

    const scheduleReconnect = () => {
      if (!isMounted || manuallyClosedRef.current) return;
      clearReconnectTimer();

      const attempt = reconnectAttemptRef.current + 1;
      reconnectAttemptRef.current = attempt;
      const baseDelay = Math.min(1000 * 2 ** (attempt - 1), 15000);
      const jitter = Math.floor(Math.random() * 500);
      const delay = baseDelay + jitter;

      console.log(`🔁 Reconnecting WebSocket in ${delay}ms (attempt ${attempt})`);
      reconnectTimeoutRef.current = setTimeout(connect, delay);
    };

    const connect = () => {
      if (!isMounted || !url) return;

      clearHeartbeat();
      const socket = new WebSocket(url);
      ws.current = socket;

      socket.onopen = () => {
        reconnectAttemptRef.current = 0;
        clearReconnectTimer();
        console.log("✅ WebSocket connected");

        heartbeatRef.current = setInterval(() => {
          if (socket.readyState === WebSocket.OPEN) {
            socket.send("");
          }
        }, 20000);
      };

      socket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessageRef.current?.(data);
        } catch (error) {
          console.error("❌ WebSocket message parse error:", error);
        }
      };

      socket.onerror = (error) => {
        if (!manuallyClosedRef.current) {
          console.error("❌ WebSocket error:", error);
        }
      };

      socket.onclose = (event) => {
        clearHeartbeat();
        if (ws.current === socket) {
          ws.current = null;
        }

        if (manuallyClosedRef.current || !isMounted) {
          console.log("🔌 WebSocket closed during cleanup");
          return;
        }

        console.log(
          `🔌 WebSocket closed code=${event.code} reason=${event.reason || "none"}`
        );
        scheduleReconnect();
      };
    };

    connect();

    return () => {
      isMounted = false;
      manuallyClosedRef.current = true;
      clearHeartbeat();
      clearReconnectTimer();
      if (
        ws.current &&
        (ws.current.readyState === WebSocket.CONNECTING ||
          ws.current.readyState === WebSocket.OPEN)
      ) {
        ws.current.close();
      }
      ws.current = null;
    };
  }, [url]);

  return ws;
};

export default useWebSocket;
