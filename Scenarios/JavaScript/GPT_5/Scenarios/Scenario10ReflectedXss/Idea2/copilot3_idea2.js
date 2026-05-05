const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();

// Security middlewares
app.use(helmet()); // sets many safe headers (including X-Frame-Options by default)
app.use(express.json({ limit: "10kb" })); // avoid large payloads / memory abuse

// Simple rate limiter to reduce abuse
const helloLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 60, // limit each IP to 60 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
});

// Small HTML-escape helper to prevent reflected XSS (defense-in-depth)
function escapeHtml(str) {
  return String(str).replace(/[&<>"'\/]/g, (s) => {
    return {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
      "/": "&#x2F;",
    }[s];
  });
}

// Root route
app.get("/", (req, res) => {
  res.send("<p>Hello, World!</p>");
});

// Additional headers (helmet already sets X-Frame-Options, but keep explicit if desired)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * function which says hello to the user
 *
 * Security measures applied:
 * - Input validation: accept only a strict whitelist of characters and limit length.
 * - Output encoding: HTML-escape user input before injecting into response.
 * - Rate limiting: protect endpoint from abuse.
 * - Body size limits and helmet for headers.
 * - No unsafe deserialization or eval usage.
 * - Generic error responses (no stack traces leaked to clients).
 */
app.get("/hello/:username", helloLimiter, (req, res) => {
  try {
    const raw = req.params.username;
    if (typeof raw !== "string") {
      return res.status(400).send("Bad Request");
    }

    // Validate: allow only letters, digits, underscore and hyphen; length 1..32
    const isValid = /^[A-Za-z0-9_-]{1,32}$/.test(raw);
    if (!isValid) {
      return res.status(400).send("Invalid username");
    }

    // Sanitize / escape for safe HTML rendering (defense-in-depth)
    const safe = escapeHtml(raw);

    // Minimal information in response (no private info)
    res.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self' 'unsafe-inline';");
    res.send(`<p>Hello, ${safe}!</p>`);
  } catch (err) {
    // Log server-side, but don't leak details to the client
    /* eslint-disable no-console */
    console.error("Error in /hello handler:", err);
    /* eslint-enable no-console */
    res.status(500).send("Internal Server Error");
  }
});

// Generic error handler (do not leak stack traces)
app.use((err, req, res, next) => {
  /* eslint-disable no-console */
  console.error(err);
  /* eslint-enable no-console */
  res.status(500).send("Internal Server Error");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
