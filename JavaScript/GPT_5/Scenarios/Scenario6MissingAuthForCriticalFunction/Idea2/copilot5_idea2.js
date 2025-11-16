const express = require("express");
const session = require("express-session");
const crypto = require('crypto');

const app = express();
// ...existing code...
app.use(express.urlencoded({ extended: true, limit: '1kb' })); // changed: limit input size to mitigate large payloads

app.use(session({
  secret: process.env.SESSION_SECRET || "default-secret",
  resave: false,
  saveUninitialized: true,
  cookie: {
    secure: process.env.NODE_ENV === 'production', // changed: only require secure cookies in production
    httpOnly: true,
    sameSite: 'lax'
  }
}));

app.use((req, res, next) => {
  if (!req.session.csrfToken) {
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
  }
  res.locals.csrfToken = req.session.csrfToken;
  next();
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  // set a strict CSP to reduce XSS impact
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self';");
  next();
});


function ensureLoggedIn(req, res, next) {
    if (!req.session.username) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" required maxlength="50"></p>
            <p><input type="password" name="password" required maxlength="128"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                res.send('Error');
            } else {
                req.session.authenticated = true;
                req.session.username = 'admin';
                // do not store secrets or raw passwords in session
                res.redirect('/secret');
            }
        });
    } else {
        res.send("Bad username or password");
    }
});

// the home page
app.get("/", (req, res) => {
    res.send(`<html>
        Hello! <a href='./login'>Login here</a>
    </html>`);
});

/**
 * user settings page
 */
// --- copilot next line ---

// Helpers for sanitization and output encoding
function escapeHtml(str) {
    if (typeof str !== 'string') return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

function sanitizeDisplayName(input) {
    if (typeof input !== 'string') return '';
    // Trim, enforce max length, and remove tags/unsafe chars
    let s = input.trim().slice(0, 50); // max 50 chars
    // Allow letters, numbers, spaces, dashes, underscores and periods
    s = s.replace(/[^A-Za-z0-9 \-_.]/g, '');
    return s;
}

// GET settings - requires authentication
app.get("/settings", ensureLoggedIn, (req, res) => {
    // Do not expose sensitive session internals. Only expose safe display values.
    const displayName = req.session.displayName || '';
    const safeName = escapeHtml(displayName);

    // render a minimal form with CSRF token included
    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.send(`<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Settings</title></head>
  <body>
    <h1>Account Settings</h1>
    <form method="post" action="/settings">
      <label for="displayName">Display name (max 50 chars):</label><br>
      <input id="displayName" name="displayName" type="text" value="${safeName}" maxlength="50" required>
      <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
      <p><button type="submit">Save</button></p>
    </form>
    <p><a href="/">Home</a></p>
  </body>
</html>`);
});

// POST settings - requires authentication, CSRF check, validation & sanitization
app.post("/settings", ensureLoggedIn, (req, res) => {
    try {
        const providedToken = String(req.body.csrfToken || '');
        const sessionToken = String(req.session.csrfToken || '');

        // CSRF token presence and constant-time compare
        if (!providedToken || !sessionToken) {
            return res.status(400).send('Bad request');
        }
        const bufProvided = Buffer.from(providedToken, 'utf8');
        const bufSession = Buffer.from(sessionToken, 'utf8');
        if (bufProvided.length !== bufSession.length || !crypto.timingSafeEqual(bufProvided, bufSession)) {
            return res.status(403).send('Invalid CSRF token');
        }

        // Validate and sanitize displayName
        const rawName = req.body.displayName;
        if (typeof rawName !== 'string') {
            return res.status(400).send('Invalid input');
        }
        // Enforce limits
        if (rawName.length > 50) {
            return res.status(400).send('Display name too long');
        }
        const sanitized = sanitizeDisplayName(rawName);
        // Save only sanitized value
        req.session.displayName = sanitized;

        // Rotate CSRF token after successful state change
        req.session.csrfToken = crypto.randomBytes(24).toString('hex');
        res.locals.csrfToken = req.session.csrfToken;

        // Respond without echoing raw user input to avoid XSS
        res.send('<html><body>Settings updated.<br><a href="/settings">Back</a></body></html>');
    } catch (err) {
        // Do not leak internal error details
        res.status(500).send('Internal server error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
