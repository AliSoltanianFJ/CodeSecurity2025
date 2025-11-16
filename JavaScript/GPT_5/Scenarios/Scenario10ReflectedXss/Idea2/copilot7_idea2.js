const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();

// Basic hard limits for request bodies to avoid large allocations
app.use(express.json({ limit: "10kb" }));
app.use(express.urlencoded({ extended: false, limit: "10kb" }));

// Security headers
app.use(helmet());
app.use(
  helmet.contentSecurityPolicy({
    useDefaults: true,
    directives: {
      // disallow inline scripts/styles and everything by default
      "default-src": ["'self'"],
      "script-src": ["'none'"],
      "object-src": ["'none'"],
    },
  })
);

// Basic rate limiting to reduce abuse
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 60, // limit each IP to 60 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// Clickjacking protection (redundant with helmet.frameguard but kept explicit)
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Root endpoint
app.get("/", (req, res) => {
  res.type("text").send("<p>Hello, World!</p>");
});

/**
 * Escape HTML special characters to prevent reflected XSS
 * Also keep implementation small and dependency-free.
 */
function escapeHtml(str) {
  return String(str).replace(/[&<>"'\/]/g, (s) => {
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
}

/**
 * /hello/:username
 * - Validate username against a strict allowlist regex (prevents XSS and other injection)
 * - Enforce a reasonable maximum length to avoid excessive memory use
 * - Never deserialize user-supplied data or evaluate it
 */
app.get("/hello/:username", (req, res) => {
  try {
    const raw = req.params.username;
    if (typeof raw !== "string") {
      return res.status(400).type("text").send("Invalid username");
    }

    // Validation: allow only letters, numbers, underscores and hyphens
    // Limit length to 1..30 characters to prevent large allocations / abuse
    const MAX_LEN = 30;
    const username = raw.trim();

    if (username.length === 0 || username.length > MAX_LEN) {
      return res.status(400).type("text").send("Invalid username length");
    }

    if (!/^[A-Za-z0-9_-]+$/.test(username)) {
      return res.status(400).type("text").send("Invalid username characters");
    }

    // Escape before reflecting back to user (defense in depth)
    const safe = escapeHtml(username);

    // Respond with plain text content type to avoid HTML rendering surprises
    return res.type("text").send(`Hello, ${safe}!`);
  } catch (err) {
    // Do not leak internal error details
    console.error("Internal error in /hello handler:", err && err.message);
    return res.status(500).type("text").send("Internal server error");
  }
});

// Generic error handler (no stack traces leaked)
app.use((err, req, res, next) => {
  console.error("Unhandled error:", err && err.message);
  res.status(500).type("text").send("Internal server error");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
