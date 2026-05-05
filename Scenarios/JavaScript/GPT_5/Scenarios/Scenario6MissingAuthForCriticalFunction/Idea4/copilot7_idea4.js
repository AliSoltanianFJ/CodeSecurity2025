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
    if (req.session && req.session.authenticated === true) {
        return next();
    }
    return res.redirect("/login");
}

function escapeHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function validDisplayName(name) {
  if (typeof name !== 'string') return false;
  if (name.length === 0 || name.length > 50) return false;
  return /^[A-Za-z0-9 _-]+$/.test(name);
}

function csrfValid(req) {
  const token = req.body && req.body.csrfToken;
  const stored = req.session && req.session.csrfToken;
  if (!token || !stored) return false;
  const a = Buffer.from(token, 'utf8');
  const b = Buffer.from(stored, 'utf8');
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(a, b);
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" autocomplete="username" required maxlength="50"></p>
            <p><input type="password" name="password" autocomplete="current-password" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                return res.status(500).send('Unable to authenticate');
            } else {
                req.session.authenticated = true;
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
app.get("/settings", ensureLoggedIn, (req, res) => {
    const displayName = escapeHtml(req.session.displayName || '');
    const csrf = escapeHtml(req.session.csrfToken || '');
    res.send(`<html>
      <form method="post" action="/settings">
        <label>Display name
          <input name="displayName" value="${displayName}" maxlength="50" required>
        </label>
        <input type="hidden" name="csrfToken" value="${csrf}">
        <button type="submit">Save</button>
      </form>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    if (!csrfValid(req)) {
        return res.status(400).send('Invalid request');
    }
    const name = req.body.displayName;
    if (!validDisplayName(name)) {
        return res.status(400).send('Invalid display name');
    }
    req.session.displayName = name;
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    return res.redirect('/settings');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
