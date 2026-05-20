import { useEffect, useState } from "react";
import API, { ensureBackendReady } from "../services/api";

const BACKEND_WAKEUP_MESSAGE =
  "Starting the backend server. The first request on free hosting can take about a minute.";

function Login({ setToken }) {
  const [isSignup, setIsSignup] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetFeedback = () => {
    setError("");
    setMessage("");
  };

  useEffect(() => {
    let cancelled = false;
    let wakeupTimer = null;

    const warmBackend = async () => {
      wakeupTimer = window.setTimeout(() => {
        if (!cancelled) {
          setStatusMessage(BACKEND_WAKEUP_MESSAGE);
        }
      }, 800);

      try {
        await ensureBackendReady();
      } catch {
        if (!cancelled) {
          setStatusMessage(BACKEND_WAKEUP_MESSAGE);
        }
      } finally {
        window.clearTimeout(wakeupTimer);
        if (!cancelled) {
          setStatusMessage("");
        }
      }
    };

    warmBackend();

    return () => {
      cancelled = true;
      if (wakeupTimer) {
        window.clearTimeout(wakeupTimer);
      }
    };
  }, []);

  const waitForBackend = async () => {
    setStatusMessage(BACKEND_WAKEUP_MESSAGE);
    await ensureBackendReady();
    setStatusMessage("");
  };

  const handleLogin = async () => {
    resetFeedback();
    setIsSubmitting(true);

    try {
      await waitForBackend();

      const res = await API.post("/api/auth/login", {
        email,
        password,
      });

      localStorage.setItem("token", res.data);
      setToken(res.data);
    } catch (err) {
      if (!err.response) {
        setError("The backend is waking up. Please wait a moment and try again.");
      } else {
        const backendMessage =
          err.response?.data?.message ||
          err.response?.data ||
          "Invalid credentials";
        setError(String(backendMessage));
      }
    } finally {
      setIsSubmitting(false);
      setStatusMessage("");
    }
  };

  const handleSignup = async () => {
    resetFeedback();
    setIsSubmitting(true);

    try {
      await waitForBackend();

      await API.post("/api/auth/register", {
        name,
        email,
        password,
      });

      setMessage("Signup successful. Please login.");
      setIsSignup(false);
      setPassword("");
    } catch (err) {
      const backendMessage =
        err.response?.data?.message ||
        err.response?.data ||
        (!err.response
          ? "The backend is waking up. Please wait a moment and try again."
          : "Signup failed");
      setError(String(backendMessage));
    } finally {
      setIsSubmitting(false);
      setStatusMessage("");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-blue-500">
      <div className="w-80 rounded bg-white p-6">
        <h2 className="mb-4 text-center text-xl">
          {isSignup ? "Signup" : "Login"}
        </h2>

        {isSignup && (
          <input
            placeholder="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mb-2 w-full border p-2"
            autoComplete="name"
          />
        )}

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mb-2 w-full border p-2"
          autoComplete="email"
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mb-2 w-full border p-2"
          autoComplete={isSignup ? "new-password" : "current-password"}
        />

        <button
          onClick={isSignup ? handleSignup : handleLogin}
          disabled={isSubmitting}
          className="w-full bg-blue-600 py-2 text-white disabled:cursor-not-allowed disabled:opacity-70"
        >
          {isSubmitting ? "Please wait..." : isSignup ? "Signup" : "Login"}
        </button>

        {statusMessage && (
          <p className="mt-2 text-sm text-slate-600">{statusMessage}</p>
        )}
        {message && <p className="mt-2 text-green-500">{message}</p>}
        {error && <p className="mt-2 text-red-500">{error}</p>}

        <p
          onClick={() => {
            setIsSignup(!isSignup);
            resetFeedback();
          }}
          className="mt-3 cursor-pointer text-center text-blue-600"
        >
          {isSignup ? "Login" : "Signup"}
        </p>
      </div>
    </div>
  );
}

export default Login;
