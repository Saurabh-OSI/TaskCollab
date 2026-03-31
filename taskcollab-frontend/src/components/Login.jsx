import { useState } from "react";
import API from "../services/api";

function Login({ setToken }) {
  const [isSignup, setIsSignup] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const resetFeedback = () => {
    setError("");
    setMessage("");
  };

  const handleLogin = async () => {
    resetFeedback();

    try {
      const res = await API.post("/api/auth/login", {
        email,
        password,
      });

      localStorage.setItem("token", res.data);
      setToken(res.data);

      // ✅ redirect after login
      window.location.href = "/";
    } catch {
      setError("Invalid credentials");
    }
  };

  const handleSignup = async () => {
    resetFeedback();

    try {
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
        "Signup failed";
      setError(String(backendMessage));
    }
  };

  return (
    <div className="min-h-screen flex justify-center items-center bg-blue-500">
      <div className="bg-white p-6 rounded w-80">
        <h2 className="text-xl mb-4 text-center">
          {isSignup ? "Signup" : "Login"}
        </h2>

        {isSignup && (
          <input
            placeholder="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="border p-2 w-full mb-2"
          />
        )}

        <input
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="border p-2 w-full mb-2"
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="border p-2 w-full mb-2"
        />

        <button
          onClick={isSignup ? handleSignup : handleLogin}
          className="bg-blue-600 text-white w-full py-2"
        >
          {isSignup ? "Signup" : "Login"}
        </button>

        {message && <p className="text-green-500 mt-2">{message}</p>}
        {error && <p className="text-red-500 mt-2">{error}</p>}

        <p
          onClick={() => {
            setIsSignup(!isSignup);
            resetFeedback();
          }}
          className="text-center mt-3 text-blue-600 cursor-pointer"
        >
          {isSignup ? "Login" : "Signup"}
        </p>
      </div>
    </div>
  );
}

export default Login;