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

const { body, validationResult } = require('express-validator');

// simple HTML escaping utility
function escapeHtml(str) {
  return (str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// GET /settings: show the form
app.get(
  '/settings',
  ensureLoggedIn,
  (req, res) => {
    // pull any existing settings out of session (or DB)
    const existing = req.session.userSettings || {};
    const email       = escapeHtml(existing.email);
    const displayName = escapeHtml(existing.displayName);
    const csrf        = res.locals.csrfToken;

    res.send(`
      <!DOCTYPE html>
      <html lang="en">
      <head><meta charset="utf-8"><title>Settings</title></head>
      <body>
        <h1>Your Settings</h1>
        <form method="POST" action="/settings">
          <input type="hidden" name="csrfToken" value="${csrf}">
          <p>
            <label>Email:
              <input
                type="email"
                name="email"
                value="${email}"
                required>
            </label>
          </p>
          <p>
            <label>Display Name:
              <input
                type="text"
                name="displayName"
                value="${displayName}"
                maxlength="50"
                required>
            </label>
          </p>
          <p><button type="submit">Save Settings</button></p>
        </form>
      </body>
      </html>
    `);
  }
);

// POST /settings: validate, sanitize, CSRF-check, save
app.post(
  '/settings',
  ensureLoggedIn,

  // express-validator middleware
  body('email')
    .isEmail().withMessage('Must be a valid email address')
    .normalizeEmail(),
  body('displayName')
    .trim()
    .isLength({ min: 1, max: 50 }).withMessage('Display name must be 1–50 chars')
    .escape(),

  (req, res) => {
    // 1) CSRF check
    if (!req.body.csrfToken || req.body.csrfToken !== req.session.csrfToken) {
      return res.status(403).send('Invalid CSRF token');
    }

    // 2) Input validation
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      // In production you’d re-render the form with error messages.
      return res.status(400).send('Invalid input: ' + JSON.stringify(errors.array()));
    }

    // 3) At this point data is validated & sanitized
    const newEmail       = req.body.email;
    const newDisplayName = req.body.displayName;

    // 4) Save the settings (here we just stick them in session;
    //    in real life you’d update your user DB record)
    req.session.userSettings = {
      email: newEmail,
      displayName: newDisplayName
    };

    // 5) Acknowledge success
    res.send(`
      <!DOCTYPE html>
      <html lang="en">
      <head><meta charset="utf-8"><title>Settings Saved</title></head>
      <body>
        <p>Your settings have been updated.</p>
        <p><a href="/settings">Back to settings</a></p>
      </body>
      </html>
    `);
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
