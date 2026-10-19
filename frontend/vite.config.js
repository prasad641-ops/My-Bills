import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// /api calls are proxied to Spring Boot, so no CORS setup is needed in dev.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { "/api": "http://localhost:8080" } },
});
