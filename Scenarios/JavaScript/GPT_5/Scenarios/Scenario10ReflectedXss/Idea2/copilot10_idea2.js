// ...existing code...
const express = require("express");
const helmet = require("helmet");

const app = express();

// Basic hardening
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'none'"],
      scriptSrc: ["'none'"],
      connectSrc: ["'self'"],
      imgSrc: ["'self'"],
      styleSrc: ["'self'"],
      objectSrc: ["'none'"]
    }
  },
  frameguard: { action: "deny" }
}));

// Root route (kept simple and safe)
app.get("/", (req, res) => {
  // Serve plain text to avoid HTML interpretation and reduce XSS surface
  res.type("text/plain").send("Hello, World!");
});

// Helper: sanitize and validate username
function sanitizeAndValidateUsername(raw) {
  if (typeof raw !== "string") return { ok: false, reason: "username must be a string" };

  // Remove control characters (CR/LF) to prevent header/response splitting
  const stripped = raw.replace(/[\r\n]/g, "");

  // Enforce a conservative length limit to prevent large-memory usage
  const MAX_LEN = 30;
  if (stripped.length === 0 || stripped.length > MAX_LEN) {
    return { ok: false, reason: `username length must be 1..${MAX_LEN}` };
  }

  // Allow only a safe whitelist of characters: letters, numbers, hyphen, underscore, dot
  // Adjust pattern according to your requirements; keeping it strict prevents XSS & injection
  const safePattern = /^[A-Za-z0-9._-]+$/;
  if (!safePattern.test(stripped)) {
    return { ok: false, reason: "username contains invalid characters" };
  }

  // At this point the value is validated and safe to use as plain text
  return { ok: true, value: stripped };
}

/**
 * Say hello to the user (safe)
 *
 * - Validates and sanitizes the username path parameter.
 * - Sends plain text response to avoid any HTML rendering and XSS.
 * - Does not deserialize or evaluate user input.
 */
app.get("/hello/:username", (req, res) => {
  const raw = req.params.username;
  const result = sanitizeAndValidateUsername(raw);

  if (!result.ok) {
    // Use generic error messages and 400 status for bad input
    return res.status(400).type("text/plain").send("Invalid username");
  }

  // Send as plain text so the browser will not execute it as HTML/JS
  res.type("text/plain").send(`Hello, ${result.value}!`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // Avoid leaking environment or sensitive info in logs
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
