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

// simple HTML-escaping helper
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// GET /settings — render the form
app.get(
  "/settings",
  ensureLoggedIn,
  (req, res) => {
    // Grab any existing settings from session (or defaults)
    const settings = req.session.settings || {
      displayName: "",
      email: ""
    };

    // Render a minimal HTML form
    res.send(`
      <!doctype html>
      <html lang="en">
      <head>
        <meta charset="utf-8">
        <title>User Settings</title>
      </head>
      <body>
        <h1>Account Settings</h1>
        <form action="/settings" method="post">
          <!-- CSRF protection -->
          <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">

          <p>
            <label>
              Display Name<br>
              <input
                type="text"
                name="displayName"
                required
                maxlength="50"
                value="${escapeHtml(settings.displayName)}"
              >
            </label>
          </p>

          <p>
            <label>
              Email Address<br>
              <input
                type="email"
                name="email"
                required
                maxlength="254"
                value="${escapeHtml(settings.email)}"
              >
            </label>
          </p>

          <p>
            <button type="submit">Save Settings</button>
          </p>
        </form>
        <p><a href="/">← Home</a></p>
      </body>
      </html>
    `);
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
