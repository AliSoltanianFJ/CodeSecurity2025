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

// utility to escape any user data we re-print into HTML
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// simple email validator
function isValidEmail(email) {
  // very simple RFC-5322-ish regexp
  return typeof email === 'string'
    && /^\S+@\S+\.\S+$/.test(email)
    && email.length <= 254;
}

// GET /settings – show the form
app.get(
  '/settings',
  ensureLoggedIn,
  (req, res) => {
    // pull any existing settings (here: from session; replace with DB lookup)
    const userSettings = req.session.userSettings || {};
    const safeEmail = escapeHtml(userSettings.email || '');
    const safeDisplayName = escapeHtml(userSettings.displayName || '');

    // render HTML with hidden CSRF token
    res.send(`
      <html>
      <head>
        <title>Your Settings</title>
      </head>
      <body>
        <h1>Account Settings</h1>
        <form method="POST" action="/settings">
          <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}" />
          
          <label>
            Email:
            <input type="email" name="email" value="${safeEmail}" required maxlength="254" />
          </label>
          <br/>

          <label>
            Display Name:
            <input type="text" name="displayName" value="${safeDisplayName}" required maxlength="50" />
          </label>
          <br/>

          <button type="submit">Save Changes</button>
        </form>
      </body>
      </html>
    `);
  }
);

// POST /settings – apply updates
app.post(
  '/settings',
  ensureLoggedIn,
  (req, res) => {
    const { csrfToken, email, displayName } = req.body;

    // 1) CSRF check
    if (!csrfToken || csrfToken !== req.session.csrfToken) {
      return res.status(403).send('Invalid CSRF token');
    }

    // 2) Basic validation
    if (!isValidEmail(email)) {
      return res.status(400).send('Please enter a valid email address.');
    }
    if (typeof displayName !== 'string'
        || displayName.length < 1
        || displayName.length > 50) {
      return res.status(400).send('Display name must be 1–50 characters.');
    }

    // 3) Save settings
    // In a real app, update your database here.
    req.session.userSettings = {
      email: email.trim(),
      displayName: displayName.trim()
    };

    // 4) Optional: rotate CSRF token so each form is single‐use
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');

    // 5) Confirm to user
    res.send(`
      <html>
      <body>
        <p>Settings updated successfully!</p>
        <p><a href="/settings">Back to Settings</a></p>
      </body>
      </html>
    `);
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
