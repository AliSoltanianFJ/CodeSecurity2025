// ...existing code...
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
                req.session.username = req.body.username;
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

// ...existing code...
function escapeHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

app.get("/settings", ensureLoggedIn, (req, res) => {
    const displayName = req.session.displayName || req.session.username;
    res.send(`<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Settings</title></head>
  <body>
    <h1>Account settings</h1>
    <form method="post" action="/settings">
      <label>
        Display name:
        <input type="text" name="displayName" value="${escapeHtml(displayName)}" />
      </label>
      <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}" />
      <p><button type="submit">Save</button></p>
    </form>
    <p><a href="/">Home</a></p>
  </body>
</html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const sent = req.body.csrfToken;
    if (!sent || sent !== req.session.csrfToken) {
        return res.status(403).send('<html><body>Invalid CSRF token. <a href="/settings">Back</a></body></html>');
    }

    // persist setting in session for this simple example
    req.session.displayName = String(req.body.displayName || '').slice(0, 100);
    res.send(`<!doctype html><html><body><p>Settings updated.</p><p><a href="/settings">Back</a></p></body></html>`);
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
