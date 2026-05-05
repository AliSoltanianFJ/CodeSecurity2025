const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");

const app = express();

// Basic security headers
app.use(helmet());
app.use((req, res, next) => {
  // reinforce frame options (explicit)
  res.setHeader("X-Frame-Options", "DENY");
  // avoid content type sniffing
  res.setHeader("X-Content-Type-Options", "nosniff");
  // restrict content sources (no inline scripts, no external resources)
  res.setHeader("Content-Security-Policy", "default-src 'self'; object-src 'none'; base-uri 'none'");
  next();
});

// Simple rate limiter to reduce abuse
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 200, // reasonable limit for this example
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

/**
 * Small, safe HTML escaper to prevent reflected XSS.
 * Replaces special characters with their HTML entity equivalents.
 */
function escapeHtml(s) {
  if (typeof s !== "string") return "";
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/\//g, "&#x2F;");
}

/**
 * Validate username:
 * - required
 * - allowed chars: letters, numbers, underscore, hyphen, dot
 * - length limit: 1..32 characters
 * - byte-length limit: <= 64 bytes (defensive)
 *
 * This prevents overly large inputs and restricts to a safe whitelist.
 */
function isValidUsername(username) {
  if (typeof username !== "string") return false;
  const len = username.length;
  if (len < 1 || len > 32) return false;
  const byteLen = Buffer.byteLength(username, "utf8");
  if (byteLen > 64) return false;
  const re = /^[A-Za-z0-9_.-]+$/;
  return re.test(username);
}

app.get("/", (req, res) => {
  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
  try {
    const rawUsername = req.params.username;

    // Validate input strictly
    if (!isValidUsername(rawUsername)) {
      // Do not echo back the invalid input; provide a generic error
      return res.status(400).send("Invalid username");
    }

    // Escape any remaining special characters before reflecting back
    const safe = escapeHtml(rawUsername);

    // Respond with safe, minimal HTML. No scripts, no user-controlled attributes.
    res.setHeader("Content-Type", "text/html; charset=utf-8");
    res.send(`<p>Hello, ${safe}!</p>`);
  } catch (err) {
    // Avoid leaking internal errors or stack traces
    console.error("Unexpected error handling /hello/:username", err);
    res.status(500).send("Internal server error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
