// ...existing code...
const express = require("express");
const session = require("express-session");
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use(session({
  secret: process.env.SESSION_SECRET || "default-secret",
  resave: false,
  saveUninitialized: true,
  cookie: {
    secure: true,
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
  // Additional hardening headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  // Content-Security-Policy prevents XSS from injected scripts
  res.setHeader('Content-Security-Policy', "default-src 'self'; object-src 'none'; frame-ancestors 'none'");
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
        <html><form action="/login" method="post" autocomplete="off">
            <p><input type="text" name="username" required maxlength="50" pattern="[A-Za-z0-9_\\-\\s]+"></p>
            <p><input type="password" name="password" required maxlength="128"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    // Basic validation and sanitisation of inputs
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    if (username === "admin" && password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                // Generic error message, do not leak internals
                res.status(500).send('Error');
            } else {
                // Set authenticated attributes safely
                req.session.authenticated = true;
                // store a minimal, validated username value
                req.session.username = 'admin';
                // refresh CSRF token after regeneration
                req.session.csrfToken = crypto.randomBytes(24).toString('hex');
                res.redirect('/secret');
            }
        });
    } else {
        res.status(401).send("Bad username or password");
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

// Helper: escape HTML to prevent XSS when echoing user-provided values
function escapeHtml(input) {
  if (typeof input !== 'string') return '';
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// Settings GET: show current (sanitised) settings, require auth
app.get("/settings", ensureLoggedIn, (req, res) => {
    const sanitizedUsername = escapeHtml(String(req.session.username || ''));
    const displayName = escapeHtml(String(req.session.displayName || ''));
    // Only show minimal info; never expose session identifiers or secrets
    res.send(`
      <html>
        <body>
          <h1>Account Settings</h1>
          <p>Signed in as: ${sanitizedUsername}</p>
          <form method="post" action="/settings">
            <label for="displayName">Display name (1-50 chars, letters/numbers/space/-/_):</label><br>
            <input id="displayName" name="displayName" type="text" value="${displayName}" maxlength="50" required pattern="[A-Za-z0-9_\\-\\s]+"><br>
            <input type="hidden" name="csrfToken" value="${escapeHtml(req.session.csrfToken)}">
            <button type="submit">Save</button>
          </form>
          <p><a href="/">Home</a></p>
        </body>
      </html>
    `);
});

// Settings POST: validate, sanitise, check CSRF, store minimal representation in session
app.post("/settings", ensureLoggedIn, (req, res) => {
    // Ensure content is expected type
    if (!req.is('application/x-www-form-urlencoded')) {
      return res.status(415).send('Unsupported media type');
    }

    const submittedCsrf = typeof req.body.csrfToken === 'string' ? req.body.csrfToken : '';
    // Constant-time compare for CSRF tokens
    const tokenA = Buffer.from(submittedCsrf);
    const tokenB = Buffer.from(String(req.session.csrfToken || ''));
    if (tokenA.length !== tokenB.length || !crypto.timingSafeEqual(Buffer.from(tokenA), Buffer.from(tokenB))) {
      return res.status(403).send('Invalid CSRF token');
    }

    let displayName = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
    // Validate length
    if (displayName.length < 1 || displayName.length > 50) {
      return res.status(400).send('Display name must be 1-50 characters');
    }
    // Allow only a safe subset of characters
    if (!/^[A-Za-z0-9_\-\s]+$/.test(displayName)) {
      return res.status(400).send('Display name contains invalid characters');
    }

    // Final sanitisation: remove any unusual control chars
    displayName = displayName.replace(/[\x00-\x1F\x7F]/g, '').substring(0, 50);

    // Store only the minimal, validated, and sanitised value
    req.session.displayName = displayName;

    // Respond with a safe confirmation page (escaped)
    res.send(`
      <html>
        <body>
          <h1>Settings updated</h1>
          <p>Display name set to: ${escapeHtml(displayName)}</p>
          <p><a href="/settings">Back to settings</a></p>
        </body>
      </html>
    `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
