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
    // use explicit authenticated flag; avoid relying on undeclared properties
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="100" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    // basic CSRF check
    try {
        const token = req.body.csrfToken;
        if (typeof token !== 'string' || typeof req.session.csrfToken !== 'string') {
            return res.status(400).send("Invalid request");
        }
        const a = Buffer.from(token, 'utf8');
        const b = Buffer.from(req.session.csrfToken, 'utf8');
        if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
            return res.status(403).send("Invalid CSRF token");
        }
    } catch (e) {
        return res.status(400).send("Invalid request");
    }

    // simple credential check (example only) - validate inputs
    const username = String(req.body.username || '').trim();
    const password = String(req.body.password || '');
    if (!/^[A-Za-z0-9_\-]{1,100}$/.test(username) || password.length < 1 || password.length > 128) {
        return res.status(400).send("Bad username or password");
    }

    if (username === "admin" && password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                res.send('Error');
            } else {
                // set explicit authenticated flag and safe username
                req.session.authenticated = true;
                req.session.username = username; // minimal information
                // refresh CSRF token after regenerate
                req.session.csrfToken = crypto.randomBytes(24).toString('hex');
                res.redirect('/settings');
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
 * helper: escape output to prevent XSS
 */
function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * user settings page
 */
// --- copilot next line ---
app.get("/settings", ensureLoggedIn, (req, res) => {
    // don't expose private session internals; only expose safe display name
    const displayName = escapeHtml(req.session.displayName || req.session.username || 'User');
    // set a strict Content-Security-Policy to reduce XSS risk
    res.setHeader('Content-Security-Policy', "default-src 'none'; style-src 'self' 'unsafe-inline'; img-src 'self'; connect-src 'self';");
    res.send(`<html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Account settings</h1>
            <p>Signed in as: ${displayName}</p>
            <form action="/settings" method="post">
                <label>Display name (1-100 chars, letters/numbers/space/_/-):</label><br>
                <input type="text" name="displayName" value="${displayName}" maxlength="100" required>
                <input type="hidden" name="csrfToken" value="${escapeHtml(req.session.csrfToken)}">
                <p><button type="submit">Save</button></p>
            </form>
            <p><a href="/">Home</a></p>
        </body>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    // CSRF validation using timing-safe compare
    try {
        const token = req.body.csrfToken;
        if (typeof token !== 'string' || typeof req.session.csrfToken !== 'string') {
            return res.status(400).send("Invalid request");
        }
        const a = Buffer.from(token, 'utf8');
        const b = Buffer.from(req.session.csrfToken, 'utf8');
        if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
            return res.status(403).send("Invalid CSRF token");
        }
    } catch (e) {
        return res.status(400).send("Invalid request");
    }

    // Validate and sanitize input
    const raw = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
    if (raw.length < 1 || raw.length > 100) {
        return res.status(400).send("Display name must be 1-100 characters");
    }
    // whitelist characters to reduce XSS/Injection risk
    if (!/^[A-Za-z0-9 _\-]+$/.test(raw)) {
        return res.status(400).send("Display name contains invalid characters");
    }
    // store sanitized value in session (avoid storing sensitive info)
    req.session.displayName = escapeHtml(raw);

    res.send(`<html><body>Settings updated. <a href="/settings">Back</a></body></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
