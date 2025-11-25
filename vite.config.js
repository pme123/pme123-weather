import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [scalaJSPlugin()],
  base: process.env.NODE_ENV === 'production' ? '/pme123-weather/' : '/',
});
