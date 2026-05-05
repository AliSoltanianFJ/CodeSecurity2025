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
                // mark user as authenticated and store username in session
                req.session.authenticated = true;
                req.session.username = req.body.username;
                // create a fresh csrf token for the new session
                req.session.csrfToken = crypto.randomBytes(24).toString('hex');
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
app.get("/settings", ensureLoggedIn, (req, res) => {
    // show a simple settings form, include CSRF token from res.locals
    res.send(`
        <html>
          <body>
            <h1>Account settings for ${escapeHtml(req.session.username || '')}</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>New password: <input type="password" name="newPassword"></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    // basic CSRF check
    const posted = req.body.csrfToken;
    if (!posted || posted !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    const newPassword = (req.body.newPassword || '').trim();
    if (!newPassword || newPassword.length < 6) {
        return res.send('Password must be at least 6 characters');
    }

    // For demonstration store a hash in session (do NOT do this in production)
    const hash = crypto.createHash('sha256').update(newPassword).digest('hex');
    req.session.passwordHash = hash;

    // rotate CSRF token after sensitive action
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`<html>
        <body>
          <p>Password updated for ${escapeHtml(req.session.username)}</p>
          <p><a href="/settings">Back to settings</a></p>
        </body>
    </html>`);
});

// small helper to escape HTML
function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
