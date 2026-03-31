import { Client } from "@stomp/stompjs";

let stompClient = null;
let isConnected = false;
let isConnecting = false;
let onConnectedCallbacks = [];
let subscriptionEntries = new Set();
let activeToken = null;

const getWebSocketUrl = () => {
  if (import.meta.env.VITE_WS_URL) {
    return import.meta.env.VITE_WS_URL;
  }

  const apiBaseUrl = import.meta.env.VITE_API_URL || "http://localhost:8080";
  return `${apiBaseUrl.replace(/^http/, "ws")}/ws`;
};

const decodeBase64Url = (value) => {
  const normalizedValue = value.replace(/-/g, "+").replace(/_/g, "/");
  const paddedValue = normalizedValue.padEnd(
    normalizedValue.length + ((4 - (normalizedValue.length % 4)) % 4),
    "="
  );

  return atob(paddedValue);
};

const getCurrentUserEmail = () => {
  const token = localStorage.getItem("token");
  if (!token) {
    return null;
  }

  const [, payload] = token.split(".");
  if (!payload) {
    return null;
  }

  try {
    const decodedPayload = JSON.parse(decodeBase64Url(payload));
    return decodedPayload.sub?.toLowerCase().trim() || null;
  } catch (error) {
    console.error("Failed to decode JWT payload for board websocket topic:", error);
    return null;
  }
};

const sanitizeEmailForTopic = (email) => email.replace(/[^a-z0-9]/g, "_");

const getBoardsTopic = () => {
  const email = getCurrentUserEmail();
  return email
    ? `/topic/boards/${sanitizeEmailForTopic(email)}`
    : null;
};

const flushConnectedCallbacks = () => {
  const callbacks = onConnectedCallbacks;
  onConnectedCallbacks = [];
  callbacks.forEach((callback) => callback?.());
};

const restoreSubscriptions = () => {
  subscriptionEntries.forEach((entry) => {
    if (!stompClient) {
      return;
    }

    entry.subscription?.unsubscribe();
    entry.subscription = entry.subscribe();
  });
};

const registerSubscription = (destination, subscribe) => {
  if (!destination) {
    return () => {};
  }

  const entry = {
    destination,
    subscribe,
    subscription: null,
  };

  subscriptionEntries.add(entry);

  if (isConnected && stompClient) {
    entry.subscription = subscribe();
  }

  return () => {
    entry.subscription?.unsubscribe();
    subscriptionEntries.delete(entry);
  };
};

export const connectWebSocket = (onConnected) => {
  if (onConnected) {
    onConnectedCallbacks.push(onConnected);
  }

  if (isConnected && stompClient) {
    flushConnectedCallbacks();
    return;
  }

  if (isConnecting) {
    return;
  }

  const token = localStorage.getItem("token");
  if (!token) {
    return;
  }

  if (activeToken && activeToken !== token) {
    disconnectWebSocket();
  }

  isConnecting = true;
  activeToken = token;

  stompClient = new Client({
    brokerURL: getWebSocketUrl(),
    reconnectDelay: 5000,
    connectionTimeout: 10000,
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  stompClient.onConnect = () => {
    isConnected = true;
    isConnecting = false;
    console.info("[WS] WebSocket connected");
    restoreSubscriptions();
    flushConnectedCallbacks();
  };

  stompClient.onDisconnect = () => {
    isConnected = false;
    isConnecting = false;
  };

  stompClient.onStompError = (frame) => {
    console.error("STOMP error:", frame.headers?.message || frame.body);
  };

  stompClient.onWebSocketClose = () => {
    isConnected = false;
    isConnecting = false;
  };

  stompClient.onWebSocketError = (error) => {
    console.error("WebSocket error:", error);
  };

  stompClient.activate();
};

export const subscribeToBoards = (onMessage) =>
  registerSubscription(getBoardsTopic(), () =>
    stompClient.subscribe(getBoardsTopic(), (message) => {
      onMessage(JSON.parse(message.body));
    })
  );

export const subscribeToBoardLists = (boardId, onMessage) =>
  registerSubscription(`/topic/board/${boardId}`, () =>
    stompClient.subscribe(`/topic/board/${boardId}`, (message) => {
      onMessage(JSON.parse(message.body));
    })
  );

export const subscribeToBoardMeta = (boardId, onMessage) =>
  registerSubscription(`/topic/board/${boardId}/meta`, () =>
    stompClient.subscribe(`/topic/board/${boardId}/meta`, (message) => {
      onMessage(JSON.parse(message.body));
    })
  );

export const subscribeToBoardActivity = (boardId, onMessage) =>
  registerSubscription(`/topic/board/${boardId}/activity`, () =>
    stompClient.subscribe(`/topic/board/${boardId}/activity`, (message) => {
      onMessage(JSON.parse(message.body));
    })
  );

export const subscribeToList = (listId, onMessage) =>
  registerSubscription(`/topic/list/${listId}`, () =>
    stompClient.subscribe(`/topic/list/${listId}`, (message) => {
      onMessage(JSON.parse(message.body));
    })
  );

export const disconnectWebSocket = async () => {
  if (!stompClient && !isConnected && !isConnecting) {
    activeToken = null;
    return;
  }

  subscriptionEntries.forEach((entry) => entry.subscription?.unsubscribe());
  subscriptionEntries = new Set();
  onConnectedCallbacks = [];
  isConnected = false;
  isConnecting = false;
  activeToken = null;

  if (stompClient) {
    await stompClient.deactivate();
    stompClient = null;
  }
};
