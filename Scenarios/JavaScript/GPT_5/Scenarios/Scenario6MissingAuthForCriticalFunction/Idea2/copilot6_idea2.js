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
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
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

// ...existing code...
// Replaced the simple settings route with authenticated, CSRF-protected,
// validated and sanitized GET + POST handlers to avoid XSS, insecure deserialization,
// buffer issues and to validate input lengths/patterns.

function escapeHtml(str) {
    if (typeof str !== 'string') return '';
    return str.replace(/[&<>"'\/]/g, function (s) {
        return ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            '/': '&#x2F;'
        })[s];
    });
}

function safeCompare(a, b) {
    try {
        const bufA = Buffer.from(String(a));
        const bufB = Buffer.from(String(b));
        if (bufA.length !== bufB.length) {
            return false;
        }
        return crypto.timingSafeEqual(bufA, bufB);
    } catch (e) {
        return false;
    }
}

// GET settings - show form with current (sanitised) values
app.get("/settings", ensureLoggedIn, (req, res) => {
    // Set a restrictive CSP to mitigate XSS and restrict what can be loaded/executed
    res.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; form-action 'self';");

    const username = escapeHtml(req.session.username || '');
    const settings = req.session.userSettings || {};
    const displayName = escapeHtml(settings.displayName || '');
    const email = escapeHtml(settings.email || '');

    // Only expose non-sensitive info and always escape output
    res.send(`
        <html>
          <head>
            <meta charset="utf-8" />
            <title>Settings</title>
          </head>
          <body>
            <h1>Settings for ${username}</h1>
            <form method="post" action="/settings">
              <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}" />
              <label>Display name: <input type="text" name="displayName" value="${displayName}" maxlength="100" /></label><br/>
              <label>Email: <input type="email" name="email" value="${email}" maxlength="254" /></label><br/>
              <button type="submit">Save</button>
            </form>
          </body>
        </html>
    `);
});

// POST settings - validate, sanitise and persist only allowed fields
app.post("/settings", ensureLoggedIn, (req, res) => {
    // Validate CSRF token exists and matches
    const incomingCsrf = req.body.csrfToken;
    if (!incomingCsrf || !safeCompare(incomingCsrf, req.session.csrfToken)) {
        return res.status(403).send('Invalid CSRF token');
    }

    // Only accept expected fields from form (no JSON parsing of user input)
    const rawDisplayName = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
    const rawEmail = typeof req.body.email === 'string' ? req.body.email.trim() : '';

    // Enforce length limits to avoid large payloads (mitigate buffer-like issues)
    if (rawDisplayName.length > 100 || rawEmail.length > 254) {
        return res.status(400).send('Input too long');
    }

    // Basic validation: displayName allow letters, numbers, spaces, hyphen, underscore
    const displayNameValid = /^[\p{L}\p{N}\s\-_\.]{0,100}$/u.test(rawDisplayName);
    // Basic email validation (RFC-complete validation is complex; use simple safe check)
    const emailValid = /^([^\s@]+)@([^\s@]+\.[^\s@]+)$/.test(rawEmail);

    if (!displayNameValid || !emailValid) {
        return res.status(400).send('Invalid input format');
    }

    // Sanitise stored values (we'll store the raw trimmed values, but always escape on output)
    req.session.userSettings = {
        displayName: rawDisplayName,
        email: rawEmail
    };

    // Rotate CSRF token after sensitive state change
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    // Do not reveal any sensitive session data; redirect to GET settings (Post/Redirect/Get)
    return res.redirect('/settings');
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
