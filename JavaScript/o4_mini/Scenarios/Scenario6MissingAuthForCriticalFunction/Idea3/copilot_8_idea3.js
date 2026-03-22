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
const crypto = require('crypto');

// simple HTML‐escape helper
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// --- SETTINGS ROUTES ---

// GET /settings: show the form
app.get('/settings', ensureLoggedIn, (req, res) => {
  // load stored settings or defaults
  const settings = req.session.settings || {};
  const email        = settings.email ? escapeHtml(settings.email) : '';
  const theme        = settings.theme === 'dark' ? 'dark' : 'light';
  const shareData    = settings.shareData ? 'checked' : '';
  const csrfToken    = res.locals.csrfToken;

  res.send(`
    <!DOCTYPE html>
    <html lang="en">
    <head><meta charset="utf-8"><title>User Settings</title></head>
    <body>
      <h1>Your Settings</h1>
      <form action="/settings" method="POST">
        <!-- CSRF protection token -->
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <p>
          <label>
            Email:
            <input type="email" name="email" required value="${email}">
          </label>
        </p>
        <p>
          <label>
            Theme:
            <select name="theme">
              <option value="light"${theme === 'light' ? ' selected' : ''}>Light</option>
              <option value="dark"${theme === 'dark' ? ' selected' : ''}>Dark</option>
            </select>
          </label>
        </p>
        <p>
          <label>
            <input type="checkbox" name="shareData" ${shareData}>
            Allow anonymous data collection
          </label>
        </p>
        <p><button type="submit">Save Settings</button></p>
      </form>
      <p><a href="/">← Home</a></p>
    </body>
    </html>
  `);
});

// POST /settings: process updates
app.post('/settings', ensureLoggedIn, (req, res) => {
  // 1) Verify CSRF token
  if (!req.body._csrf || req.body._csrf !== req.session.csrfToken) {
    return res.status(403).send('Invalid CSRF token');
  }

  // 2) Extract and validate inputs
  const rawEmail     = (req.body.email || '').trim();
  const rawTheme     = req.body.theme;
  const rawShareData = req.body.shareData;

  // basic email check (you can swap in a more robust validator)
  const emailRegex = /^\S+@\S+\.\S+$/;
  if (!emailRegex.test(rawEmail)) {
    return res.status(400).send('Please provide a valid email address.');
  }

  // only allow these two choices
  const theme = rawTheme === 'dark' ? 'dark' : 'light';

  // checkbox is present === truthy
  const shareData = rawShareData === 'on';

  // 3) Save into session (or your real DB)
  req.session.settings = {
    email: rawEmail,
    theme,
    shareData
  };

  // 4) Rotate CSRF token so a single token can’t be reused
  req.session.csrfToken = crypto.randomBytes(24).toString('hex');

  // 5) Redirect back to GET /settings (Post/Redirect/Get)
  res.redirect('/settings');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
