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

// In-memory demo user store (DO NOT use this in production)
const users = {};

// helper: create a password entry with scrypt
function hashPassword(password) {
  const salt = crypto.randomBytes(16);
  const derived = crypto.scryptSync(password, salt, 64);
  return {
    salt: salt.toString('hex'),
    hash: derived.toString('hex')
  };
}

// helper: verify password using scrypt and timing-safe compare
function verifyPassword(password, userEntry) {
  if (!userEntry || !userEntry.salt || !userEntry.hash) return false;
  const salt = Buffer.from(userEntry.salt, 'hex');
  const derived = crypto.scryptSync(password, salt, 64);
  const known = Buffer.from(userEntry.hash, 'hex');
  if (derived.length !== known.length) return false;
  return crypto.timingSafeEqual(derived, known);
}

// safe compare for CSRF tokens
function safeCompare(a, b) {
  try {
    const A = Buffer.from(String(a), 'utf8');
    const B = Buffer.from(String(b), 'utf8');
    if (A.length !== B.length) return false;
    return crypto.timingSafeEqual(A, B);
  } catch (e) {
    return false;
  }
}

// small helper to escape HTML
function escapeHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

// initialize demo admin user (for the existing login flow)
if (!users.admin) {
  const entry = hashPassword('password');
  users.admin = entry;
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" required></p>
            <p><input type="password" name="password" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    const username = (req.body.username || '').trim();
    const password = req.body.password || '';

    // basic validation
    if (!username || !password) {
      return res.send("Bad username or password");
    }

    const userEntry = users[username];
    if (!userEntry || !verifyPassword(password, userEntry)) {
      return res.send("Bad username or password");
    }

    req.session.regenerate(function (err) {
        if (err) {
            res.send('Error');
        } else {
            req.session.authenticated = true;
            req.session.username = username;
            // rotate CSRF token on login
            req.session.csrfToken = crypto.randomBytes(24).toString('hex');
            res.redirect('/secret');
        }
    });
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
app.get("/settings", ensureLoggedIn, (req, res) => {
    // show a simple settings form, include CSRF token from res.locals
    res.send(`
        <html>
          <head><meta charset="utf-8"></head>
          <body>
            <h1>Account settings for ${escapeHtml(req.session.username || '')}</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
              <p>New password: <input type="password" name="newPassword" minlength="8" required></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    // CSRF check
    const posted = req.body.csrfToken;
    if (!posted || !req.session.csrfToken || !safeCompare(posted, req.session.csrfToken)) {
        return res.status(403).send('Invalid CSRF token');
    }

    const newPassword = (req.body.newPassword || '').trim();
    if (!newPassword || newPassword.length < 8) {
        return res.send('Password must be at least 8 characters');
    }

    const username = req.session.username;
    if (!username) {
      return res.status(401).send('Not authenticated');
    }

    // update stored password entry for the user
    const newEntry = hashPassword(newPassword);
    users[username] = newEntry;

    // rotate CSRF token after sensitive action
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`<html>
        <body>
          <p>Password updated for ${escapeHtml(username)}</p>
          <p><a href="/settings">Back to settings</a></p>
        </body>
    </html>`);
});

// a small protected secret page referenced by login redirect
app.get("/secret", ensureLoggedIn, (req, res) => {
    res.send(`<html><body><h1>Secret area</h1><p>Welcome ${escapeHtml(req.session.username)}</p><p><a href="/settings">Settings</a></p></body></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
