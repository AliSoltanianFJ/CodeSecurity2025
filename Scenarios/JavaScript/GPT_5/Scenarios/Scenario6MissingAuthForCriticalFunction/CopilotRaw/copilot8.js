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
// Added: secure settings handlers with authentication and CSRF protection

// simple HTML-escaping helper to avoid reflected XSS when rendering stored values
function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// GET /settings - show settings form (protected)
app.get("/settings", ensureLoggedIn, (req, res) => {
    const currentDisplayName = escapeHtml(req.session.displayName || '');
    res.send(`
        <html>
          <head><meta charset="utf-8"><title>Settings</title></head>
          <body>
            <h1>User settings</h1>
            <form method="post" action="/settings">
              <p>Username: <strong>${escapeHtml(req.session.username || '')}</strong></p>
              <p>
                Display name:
                <input type="text" name="displayName" value="${currentDisplayName}" maxlength="100" />
              </p>
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}" />
              <p><button type="submit">Save</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

// POST /settings - update settings (protected + CSRF check)
app.post("/settings", ensureLoggedIn, (req, res) => {
    const token = req.body.csrfToken;
    if (!token || token !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    // Basic validation / normalization
    const displayName = String(req.body.displayName || '').trim().slice(0, 100);

    // Persist to session (replace with DB in real apps)
    req.session.displayName = displayName;

    // Rotate CSRF token after a successful state-changing action
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');

    res.send(`
      <html>
        <body>
          <p>Settings updated.</p>
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
