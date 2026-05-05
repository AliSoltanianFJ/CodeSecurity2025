// ...existing code...
const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const app = express();

// Basic hardening
app.use(helmet());

// Rate limiting to mitigate abuse
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 60,                 // limit each IP to 60 requests per windowMs
  standardHeaders: true,
  legacyHeaders: false
});
app.use(limiter);

// Root endpoint
app.get("/", (req, res) => {
    res.type('text/html').send("<p>Hello, World!</p>");
});

// Prevent clickjacking (kept explicit as in original)
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 *
 * Security measures:
 * - Validate input (allowed characters + length)
 * - Limit length to prevent memory abuse
 * - Decode URI component safely
 * - Sanitize/escape output to prevent XSS
 * - Avoid insecure deserialization (no eval / JSON.parse on user input)
 */
app.get("/hello/:username", (req, res) => {
    const raw = req.params.username || '';

    // Safely decode; reject invalid percent-encoding
    let username;
    try {
        username = decodeURIComponent(raw);
    } catch (err) {
        return res.status(400).json({ error: 'Invalid username encoding' });
    }

    // Enforce length limits to avoid excessive memory usage
    if (username.length === 0 || username.length > 30) {
        return res.status(400).json({ error: 'Username must be 1-30 characters' });
    }

    // Allow only a safe subset of characters to prevent injection (alphanum, underscore, dash)
    const allowed = /^[A-Za-z0-9_\-]+$/;
    if (!allowed.test(username)) {
        return res.status(400).json({ error: 'Username contains invalid characters' });
    }

    // Simple HTML escape to ensure any downstream HTML contexts remain safe
    const escapeHtml = (s) => s.replace(/[&<>"'\/]/g, (c) => {
        return ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            '/': '&#x2F;'
        })[c];
    });

    const safeUsername = escapeHtml(username);

    // Return structured JSON (safer than embedding raw HTML)
    return res.type('application/json').status(200).json({ message: `Hello, ${safeUsername}!` });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
