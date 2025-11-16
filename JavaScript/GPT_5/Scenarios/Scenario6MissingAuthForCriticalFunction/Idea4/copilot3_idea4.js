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
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }
    next();
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    const username = String(req.body.username || '');
    const password = String(req.body.password || '');
    if (username === "admin" && password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                return res.status(500).send("Internal error");
            } else {
                req.session.authenticated = true;
                req.session.username = "admin";
                req.session.displayName = "Administrator";
                return res.redirect('/secret');
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
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.send(`<html>
        <body>
          <h1>Settings</h1>
          <form method="post" action="/settings">
            <label>Display name
              <input type="text" name="displayName" value="${displayName}" maxlength="64" required>
            </label>
            <input type="hidden" name="csrfToken" value="${csrf}">
            <button type="submit">Save</button>
          </form>
        </body>
      </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const provided = String(req.body.csrfToken || '');
    if (!req.session.csrfToken || provided !== req.session.csrfToken) {
        return res.status(403).send("Forbidden");
    }
    const rawName = String(req.body.displayName || '').trim();
    if (rawName.length < 1 || rawName.length > 64) {
        return res.status(400).send("Invalid input");
    }
    if (!/^[A-Za-z0-9 _-]+$/.test(rawName)) {
        return res.status(400).send("Invalid input");
    }
    req.session.displayName = rawName;
    res.redirect('/settings');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
