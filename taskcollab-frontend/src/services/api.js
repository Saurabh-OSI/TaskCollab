import axios from "axios";

// ✅ Base API instance
const API = axios.create({
  baseURL: "https://taskcollab-production.up.railway.app",
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ Attach JWT token in every request
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// ✅ Handle token expiry / unauthorized globally
API.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    const isAuthRequest =
      error.config?.url?.includes("/api/auth/login") ||
      error.config?.url?.includes("/api/auth/register");

    // 🔒 Handle expired/invalid token
    if (!isAuthRequest && (status === 401 || status === 403)) {
      console.warn("🔒 Session expired → Logging out");

      localStorage.removeItem("token");

      // Redirect to login/home page
      window.location.href = "/";
    }

    return Promise.reject(error);
  }
);

export default API;
