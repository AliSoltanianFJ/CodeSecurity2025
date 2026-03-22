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

// add this near the top of your file:
const crypto = require('crypto')

// simple HTML‐escaping utility
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// GET /settings – show the form
app.get('/settings', ensureLoggedIn, (req, res) => {
  // pull any previously saved settings (in a real app these would
  // come from your user database, not from the session)
  const settings = req.session.settings || {
    displayName: '',
    email: ''
  }

  // escape everything before re‐injecting into the page
  const displayNameEscaped = escapeHtml(settings.displayName)
  const emailEscaped       = escapeHtml(settings.email)
  const csrfToken          = res.locals.csrfToken

  res.send(`
    <html>
      <head><title>Your Settings</title></head>
      <body>
        <h1>Account Settings</h1>
        <form action="/settings" method="post">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <p>
            <label>
              Display Name:
              <input
                type="text"
                name="displayName"
                value="${displayNameEscaped}"
                maxlength="50"
                required>
            </label>
          </p>
          <p>
            <label>
              Email Address:
              <input
                type="email"
                name="email"
                value="${emailEscaped}"
                required>
            </label>
          </p>
          <p>
            <button type="submit">Save Settings</button>
          </p>
        </form>
      </body>
    </html>
  `)
})

// POST /settings – process form submission
app.post('/settings', ensureLoggedIn, (req, res) => {
  // 1) CSRF check
  const sentToken = req.body._csrf
  if (!sentToken || sentToken !== req.session.csrfToken) {
    return res.status(403).send('Invalid CSRF token')
  }

  // 2) Simple validation
  const rawDisplayName = req.body.displayName || ''
  const rawEmail       = req.body.email       || ''

  if (rawDisplayName.length < 1 || rawDisplayName.length > 50) {
    return res.status(400).send('Display Name must be 1–50 characters')
  }

  // very basic email regex – you can replace this with a more robust one
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRe.test(rawEmail)) {
    return res.status(400).send('Invalid email address')
  }

  // 3) Sanitize / escape
  const displayName = escapeHtml(rawDisplayName)
  const email       = escapeHtml(rawEmail)

  // 4) “Persist” the settings
  // In a real app you’d write these to your user database.
  // For demo purposes we’ll keep them in the session.
  req.session.settings = { displayName, email }

  // 5) Feedback
  res.send(`
    <html>
      <head><title>Settings Saved</title></head>
      <body>
        <p>Your settings have been updated.</p>
        <p><a href="/settings">Back to settings</a></p>
      </body>
    </html>
  `)
})

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
