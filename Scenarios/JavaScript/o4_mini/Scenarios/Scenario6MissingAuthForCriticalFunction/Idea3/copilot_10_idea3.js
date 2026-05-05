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

// simple helper to HTML-escape any user-supplied string
function escapeHTML(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ensureLoggedIn is already defined above
// GET /settings — show the form
app.get("/settings", ensureLoggedIn, (req, res) => {
  // pull any existing settings from session (or fall back to username)
  const settings = req.session.settings || {};
  const displayName = escapeHTML(settings.displayName || req.session.username || '');

  // render a simple settings form, including the CSRF token
  res.send(`
    <html>
    <head>
      <title>Your Settings</title>
    </head>
    <body>
      <h1>Account Settings</h1>
      <form action="/settings" method="post">
        <input type="hidden" name="csrfToken" value="${escapeHTML(res.locals.csrfToken)}">
        <p>
          <label for="displayName">Display Name:</label><br>
          <input type="text" id="displayName" name="displayName" 
                 value="${displayName}" 
                 maxlength="50" 
                 required>
        </p>
        <p><button type="submit">Save Settings</button></p>
      </form>
      <p><a href="/">Back to Home</a></p>
    </body>
    </html>
  `);
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
