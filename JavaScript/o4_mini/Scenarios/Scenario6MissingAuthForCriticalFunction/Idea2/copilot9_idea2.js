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

const escapeHtml = require('escape-html');
const { body, validationResult } = require('express-validator');
// ...existing code...
// --- copilot next line ---
app.get("/settings", ensureLoggedIn, (req, res) => {
    const settings = req.session.settings || {};
    const email = escapeHtml(settings.email || '');
    const displayName = escapeHtml(settings.displayName || '');
    res.send(`
        <html><body>
            <form method="post" action="/settings">
                <input type="hidden" name="_csrf" value="${res.locals.csrfToken}">
                <p><label>Email: <input type="email" name="email" value="${email}" required></label></p>
                <p><label>Display Name: <input type="text" name="displayName" value="${displayName}" maxlength="50"></label></p>
                <p><button type="submit">Update Settings</button></p>
            </form>
        </body></html>
    `);
});

app.post("/settings",
    ensureLoggedIn,
    body('_csrf').custom((value, { req }) => {
        if (value !== req.session.csrfToken) throw new Error('Invalid CSRF token');
        return true;
    }),
    body('email').isEmail().normalizeEmail(),
    body('displayName').trim().isLength({ max: 50 }).escape(),
    (req, res) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) return res.status(400).send('Invalid input');
        req.session.settings = {
            email: req.body.email,
            displayName: req.body.displayName
        };
        res.send('Settings updated successfully');
    }
);
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
