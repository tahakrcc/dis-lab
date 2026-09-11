import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
// Dev sunucusu 5173'te; /api istekleri Spring Boot backend'ine (8080) proxy'lenir.
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
});
