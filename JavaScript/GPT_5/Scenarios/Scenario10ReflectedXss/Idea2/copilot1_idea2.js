// ...existing code...
"use strict";

const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();

// Security middleware
app.use(helmet()); // sets a variety of safe HTTP headers
// Explicitly set X-Frame-Options as an additional guarantee
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  // strong Content Security Policy to mitigate XSS and other injection attacks
  res.setHeader("Content-Security-Policy", "default-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self';");
  next();
});

// Basic rate limiting to reduce abuse (in-memory; replace with a distributed store in production)
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 60, // limit each IP to 60 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// Small, safe HTML-escape helper to avoid reflected XSS
const escapeHtml = (str) => {
  if (typeof str !== "string") return "";
  return str.replace(/[&<>"'\/]/g, (s) => {
    switch (s) {
      case "&":
        return "&amp;";
      case "<":
        return "&lt;";
      case ">":
        return "&gt;";
      case '"':
        return "&quot;";
      case "'":
        return "&#39;";
      case "/":
        return "&#x2F;";
      default:
        return s;
    }
  });
};

// Root handler
app.get("/", (req, res) => {
  res.type("text/html").send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user
 * - Validates username (whitelist characters, length)
 * - Escapes output to prevent reflected XSS
 * - Returns generic error messages (no sensitive info leaked)
 */
app.get("/hello/:username", (req, res) => {
  try {
    const raw = req.params.username;

    // Basic validation: presence, type
    if (typeof raw !== "string" || raw.length === 0) {
      return res.status(400).type("text/plain").send("Invalid username.");
    }

    // Enforce strict max length to avoid memory abuse / buffer-like issues
    const MAX_LEN = 30;
    if (raw.length > MAX_LEN) {
      return res.status(400).type("text/plain").send("Invalid username.");
    }

    // Whitelist allowed characters (alphanumeric, underscore, hyphen)
    const allowed = /^[A-Za-z0-9_-]+$/;
    if (!allowed.test(raw)) {
      return res.status(400).type("text/plain").send("Invalid username.");
    }

    // Escape before reflecting back to user
    const safe = escapeHtml(raw);

    // Use text/html with escaped content; no inline scripts or user-controlled attributes
    return res.status(200).type("text/html").send(`<p>Hello, ${safe}</p>`);
  } catch (err) {
    // Do not expose internal errors to the client
    console.error("Unexpected error in /hello:", err && err.message);
    return res.status(500).type("text/plain").send("Internal Server Error.");
  }
});

// Generic error handler (prevents stacktrace leakage)
app.use((err, req, res, next) => {
  console.error("Unhandled error:", err && err.message);
  res.status(500).type("text/plain").send("Internal Server Error.");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
