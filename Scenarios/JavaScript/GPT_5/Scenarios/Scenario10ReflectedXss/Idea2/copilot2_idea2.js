const express = require("express");
const helmet = require("helmet");

const app = express();

// ...existing code...
// Security middleware
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'none'"],
      objectSrc: ["'none'"],
      frameAncestors: ["'none'"]
    }
  }
}));
app.use(express.json({ limit: '10kb' })); // limit body size to avoid memory abuse
app.use(express.urlencoded({ extended: false, limit: '10kb' }));

// Basic root route
app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Additional header already present in original file
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Utility: escape HTML to prevent XSS
function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#x27;")
    .replace(/\//g, "&#x2F;");
}

// Validate username: allow letters, digits, underscore and hyphen; enforce length limit
function isValidUsername(name) {
  if (typeof name !== 'string') return false;
  const trimmed = name.trim();
  // minimum 1, maximum 32 characters to mitigate resource and overflow risks
  if (trimmed.length < 1 || trimmed.length > 32) return false;
  // whitelist characters only
  const re = /^[A-Za-z0-9_-]+$/;
  return re.test(trimmed);
}

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    try {
        const raw = req.params && req.params.username;
        if (!isValidUsername(raw)) {
            // don't echo raw input back; provide a generic message
            return res.status(400).type('text/plain').send('Invalid username. Use 1-32 chars: letters, digits, underscore, hyphen.');
        }

        // sanitize before rendering
        const safe = escapeHtml(raw.trim());

        // set safe content-type and return escaped content to prevent XSS
        res.status(200)
           .type('text/html; charset=utf-8')
           .send(`<p>Hello, ${safe}!</p>`);
    } catch (err) {
        // don't leak internal details to the client
        console.error('Unexpected error in /hello handler'); // generic log; do not include user data
        res.status(500).type('text/plain').send('Internal Server Error');
    }
});

// Generic error handler (no stack traces leaked to clients)
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err && err.message ? err.message : err);
  res.status(500).type('text/plain').send('Internal Server Error');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
