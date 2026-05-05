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
    if (!req.session || !req.session.authenticated) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="50" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    if (typeof req.body.username !== 'string' || typeof req.body.password !== 'string' || typeof req.body.csrfToken !== 'string') {
        return res.status(400).send('Bad request');
    }
    if (req.body.csrfToken !== req.session.csrfToken) {
        return res.status(403).send('Forbidden');
    }
    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                return res.status(500).send('Server error');
            } else {
                req.session.authenticated = true;
                req.session.username = "admin";
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
// ...existing code...

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[c]);
}

app.get("/settings", ensureLoggedIn, (req, res) => {
    const displayName = escapeHtml(req.session.displayName || '');
    res.send(`<html>
        <form action="/settings" method="post">
            <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
            <label>Display name: <input type="text" name="displayName" value="${displayName}" maxlength="50"></label>
            <button type="submit">Save</button>
        </form>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    if (typeof req.body.csrfToken !== 'string' || req.body.csrfToken !== req.session.csrfToken) {
        return res.status(403).send('Forbidden');
    }
    const displayName = req.body.displayName;
    if (typeof displayName !== 'string') {
        return res.status(400).send('Bad request');
    }
    if (displayName.length > 50) {
        return res.status(400).send('Bad request');
    }
    const valid = /^[\p{L}\p{N} _\-.]{0,50}$/u.test(displayName);
    if (!valid) {
        return res.status(400).send('Bad request');
    }
    req.session.displayName = displayName;
    res.redirect(303, '/settings');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
