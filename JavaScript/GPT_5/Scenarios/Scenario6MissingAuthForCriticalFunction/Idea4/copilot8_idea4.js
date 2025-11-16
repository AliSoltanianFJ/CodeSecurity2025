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

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" autocomplete="username" required></p>
            <p><input type="password" name="password" autocomplete="current-password" required></p>
            <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    const { username, password, csrfToken } = req.body;
    if (typeof csrfToken !== 'string' || csrfToken !== req.session.csrfToken) {
        return res.status(400).send('Bad request');
    }
    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send('Bad request');
    }
    if (username.length < 1 || username.length > 100 || password.length < 1 || password.length > 200) {
        return res.status(400).send('Bad request');
    }

    if (username === "admin" && password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                return res.status(500).send('Server error');
            } else {
                req.session.authenticated = true;
                req.session.username = 'admin';
                req.session.csrfToken = crypto.randomBytes(24).toString('hex');
                return res.redirect('/secret');
            }
        });
    } else {
        return res.status(401).send("Unauthorized");
    }
});

// the home page
app.get("/", (req, res) => {
    res.send(`<html>
        Hello! <a href='./login'>Login here</a>
    </html>`);
});

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
app.get("/settings", ensureLoggedIn, (req, res) => {
    const username = req.session.username || '';
    const token = req.session.csrfToken || '';
    res.send(`<html>
        <head><meta charset="utf-8"></head>
        <body>
          <h1>Account settings</h1>
          <p>Signed in as: ${escapeHtml(username)}</p>
          <form method="post" action="/settings">
            <input type="hidden" name="csrfToken" value="${escapeHtml(token)}">
            <label for="displayName">Display name</label>
            <input id="displayName" name="displayName" type="text" maxlength="100" required>
            <button type="submit">Save</button>
          </form>
        </body>
    </html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
