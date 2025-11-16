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
    // require an authenticated session flag rather than a username property
    if (!req.session || !req.session.authenticated) {
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
                // store username for convenience (optional)
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
function escapeHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

// GET /settings - show settings form (requires authentication)
app.get("/settings", ensureLoggedIn, (req, res) => {
    const currentEmail = escapeHtml(req.session.email || '');
    res.send(`
        <html>
          <head><meta charset="utf-8"></head>
          <body>
            <h2>Account settings</h2>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>
                <label>Email: <input type="email" name="email" value="${currentEmail}" required></label>
              </p>
              <p><button type="submit">Save</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

// POST /settings - update settings with CSRF protection and basic validation
app.post("/settings", ensureLoggedIn, (req, res) => {
    const token = req.body.csrfToken;
    if (!token || token !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    const email = String(req.body.email || '').trim();
    // basic email validation
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).send('Invalid email address');
    }

    // persist to session (replace with DB/save in real app)
    req.session.email = email;

    // rotate CSRF token after sensitive change (optional)
    req.session.csrfToken = crypto.randomBytes(24).toString('hex');
    res.locals.csrfToken = req.session.csrfToken;

    res.send(`<html><p>Settings updated.</p><p><a href="/settings">Back to settings</a></p></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
