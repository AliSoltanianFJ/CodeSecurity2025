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
  next();
});


function ensureLoggedIn(req, res, next) {
    if (!req.session.username || !req.session.authenticated) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" autocomplete="username"></p>
            <p><input type="password" name="password" autocomplete="current-password"></p>
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
                // set a minimal, non-sensitive username marker
                req.session.username = "admin";
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

// Helper: simple and safe HTML-escaper to prevent XSS when rendering user data
function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// Validate email with a conservative pattern (not perfect but avoids JS engine special objects)
function isValidEmail(email) {
  if (typeof email !== 'string') return false;
  // Basic RFC-5322-ish-ish lightweight check
  return /^[^\s@]{1,64}@[^\s@]{1,255}\.[^\s@]{2,63}$/.test(email);
}

// GET settings page - require authentication
app.get("/settings", ensureLoggedIn, (req, res) => {
    // set a restrictive CSP to reduce XSS risk
    res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");
    const settings = req.session.settings || {};
    const safeDisplayName = escapeHtml(settings.displayName || '');
    const safeEmail = escapeHtml(settings.email || '');

    res.send(`<html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Account settings</h1>
            <form action="/settings" method="post">
                <input type="hidden" name="csrfToken" value="${escapeHtml(req.session.csrfToken)}">
                <label>Display name: <input type="text" name="displayName" value="${safeDisplayName}" maxlength="50" /></label><br/>
                <label>Email: <input type="email" name="email" value="${safeEmail}" maxlength="254" /></label><br/>
                <button type="submit">Save</button>
            </form>
        </body>
    </html>`);
});

// POST settings update - require authentication, validate and sanitize input, verify CSRF
app.post("/settings", ensureLoggedIn, (req, res) => {
    try {
        // CSRF token validation
        const token = req.body.csrfToken;
        if (!token || token !== req.session.csrfToken) {
            return res.status(403).send('Invalid CSRF token');
        }

        // Extract inputs
        const rawName = String(req.body.displayName || '').trim();
        const rawEmail = String(req.body.email || '').trim();

        // Validate lengths to prevent excessive memory usage
        if (rawName.length > 50) return res.status(400).send('Display name too long');
        if (rawEmail.length > 254) return res.status(400).send('Email too long');

        // Validate email format
        if (rawEmail && !isValidEmail(rawEmail)) {
            return res.status(400).send('Invalid email address');
        }

        // Sanitize by escaping before storing/displaying (store only safe strings)
        const safeName = escapeHtml(rawName);
        const safeEmail = escapeHtml(rawEmail);

        // Persist settings in session (keep stored data minimal and non-sensitive)
        req.session.settings = {
            displayName: safeName,
            email: safeEmail
        };

        // Rotate CSRF token after a privileged operation
        req.session.csrfToken = crypto.randomBytes(24).toString('hex');

        res.send(`<html>
            <head><meta charset="utf-8"></head>
            <body>
                <p>Settings updated.</p>
                <a href="/settings">Back to settings</a>
            </body>
        </html>`);
    } catch (err) {
        // Generic error message to avoid leaking internal details
        res.status(500).send('An error occurred');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
