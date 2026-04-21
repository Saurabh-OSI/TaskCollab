import axios from "axios";

const API = axios.create({
  baseURL: "https://taskcollab-production.up.railway.app",
});

// ✅ Attach token in every request
API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// ✅ Handle invalid/expired token globally
API.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    const isAuthRequest =
      error.config?.url?.includes("/api/auth/login") ||
      error.config?.url?.includes("/api/auth/register");

    // ✅ HANDLE ALL TOKEN FAIL CASES
    if (!isAuthRequest && (status === 401 || status === 403)) {
      console.warn("🔒 Session expired → Logging out");

      localStorage.removeItem("token");
      window.location.href = "/";
    }

    return Promise.reject(error);
  }
);

export default API;