// ...existing code...
const express = require("express");
const helmet = require("helmet");

const app = express();

// Basic hard limits to avoid large payloads / memory exhaustion and minimize deserialization risks
app.use(express.json({ limit: "1kb", strict: true }));
app.use(express.urlencoded({ extended: false, limit: "1kb" }));

// Secure headers
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      // allow inline styles only if you must; for now we avoid inline scripts/styles
      styleSrc: ["'self'"]
    }
  },
  // other helmet protections enabled by default
}));

// Additional safe headers
app.use((req, res, next) => {
  res.setHeader("X-Content-Type-Options", "nosniff");
  // Frame options already covered by helmet; keep explicit denial as an extra layer
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/", (req, res) => {
  res.type("text/html").send("<p>Hello, World!</p>");
});

// Minimal, safe HTML-escape function to prevent reflected XSS
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;")
    .replace(/\//g, "&#x2F;");
}

/**
 * Say hello to a validated username.
 *
 * Security measures:
 *  - Validate username against a strict allowlist regex (alphanumeric, hyphen, underscore)
 *  - Enforce a reasonable maximum length to avoid large allocations
 *  - Escape output to prevent XSS
 *  - Avoid any eval/JSON.parse of user input (no insecure deserialization)
 */
app.get("/hello/:username", (req, res) => {
  // Treat param as string and trim whitespace
  const raw = String(req.params.username || "").trim();

  // Strict validation: only letters, numbers, hyphen, underscore; length 1..30
  const isValid = /^[A-Za-z0-9_-]{1,30}$/.test(raw);
  if (!isValid) {
    // Return minimal error info, do not leak internal details
    return res.status(400).type("text/plain").send("Invalid username");
  }

  // Sanitize / escape before embedding into HTML
  const safe = escapeHtml(raw);

  // Return safe HTML with explicit content-type
  res.type("text/html").send(`<p>Hello, ${safe}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});