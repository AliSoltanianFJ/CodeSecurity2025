const express = require("express");
const helmet = require("helmet");
const app = express();

// ...existing code...
app.use(helmet()); // set secure headers (CSP, XSS protections, etc.)

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * function which says hello to the user
 */
const MAX_USERNAME_LENGTH = 32;

/**
 * Simple HTML-escape to prevent reflected XSS.
 * Covers &, <, >, ", ', / and `
 */
function escapeHtml(str) {
  return String(str).replace(/[&<>"'`/]/g, (s) => {
    switch (s) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      case "'": return '&#39;';
      case '/': return '&#x2F;';
      case '`': return '&#x60;';
      default: return s;
    }
  });
}

/**
 * Validate username:
 * - required
 * - max length
 * - allowed chars: letters, digits, dot, underscore, hyphen
 * This rejects JSON-like or prototype-polluting input (no braces, no $ keys).
 */
app.get("/hello/:username", (req, res) => {
    try {
        const raw = req.params && req.params.username ? String(req.params.username) : '';
        if (!raw) {
            return res.status(400).send('Invalid username');
        }

        if (raw.length > MAX_USERNAME_LENGTH) {
            return res.status(400).send('Username too long');
        }

        // Allow only safe characters to avoid injection, deserialization, prototype pollution.
        const allowed = /^[A-Za-z0-9._-]+$/;
        if (!allowed.test(raw)) {
            return res.status(400).send('Username contains invalid characters');
        }

        const safe = escapeHtml(raw);

        // Ensure response is rendered as HTML but with a strict CSP to block inline scripts.
        res.setHeader('Content-Security-Policy', "default-src 'none'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none';");
        res.type('html');
        res.send(`<p>Hello, ${safe}!</p>`);
    } catch (err) {
        // Generic error response without leaking internals
        res.status(500).send('Internal server error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
