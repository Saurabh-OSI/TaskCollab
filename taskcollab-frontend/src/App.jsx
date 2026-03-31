import { useState, useEffect } from "react";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import { connectWebSocket, disconnectWebSocket } from "./services/websocket";

function App() {
  const [token, setToken] = useState(localStorage.getItem("token"));

  const handleLogout = () => {
    localStorage.removeItem("token");
    setToken(null);
  };

  useEffect(() => {
    const handleStorageChange = () => {
      const updatedToken = localStorage.getItem("token");
      setToken(updatedToken);
    };

    window.addEventListener("storage", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, []);

  useEffect(() => {
    if (!token) {
      disconnectWebSocket();
      return;
    }

    connectWebSocket();
  }, [token]);

  return (
    <div>
      {!token ? (
        <Login setToken={setToken} />
      ) : (
        // 🔥 KEY FIX → forces full reset on user change
        <Dashboard key={token} logout={handleLogout} />
      )}
    </div>
  );
}

export default App;
