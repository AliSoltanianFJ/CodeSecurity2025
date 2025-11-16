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
app.get("/settings", ensureLoggedIn, (req, res) => {
    res.send(`<html>
      <body>
        <h1>Account settings</h1>
        <p>Signed in as ${req.session.username || 'unknown'}</p>

        <form method="post" action="/settings">
          <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
          <p>Display name: <input type="text" name="displayName" value="${req.session.displayName || ''}"></p>
          <p><button type="submit">Save</button></p>
        </form>

        <form method="post" action="/logout" style="margin-top:1rem">
          <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
          <button type="submit">Logout</button>
        </form>
      </body>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
  // simple CSRF check
  if (!req.body.csrfToken || req.body.csrfToken !== req.session.csrfToken) {
    return res.status(403).send('Invalid CSRF token');
  }

  // basic validation and sanitization
  const displayName = String(req.body.displayName || '').trim().slice(0, 100);
  req.session.displayName = displayName;

  res.send(`<html>Settings saved. <a href="/settings">Back</a></html>`);
});

app.post("/logout", ensureLoggedIn, (req, res) => {
  if (!req.body.csrfToken || req.body.csrfToken !== req.session.csrfToken) {
    return res.status(403).send('Invalid CSRF token');
  }

  req.session.destroy(err => {
    // ignore error and redirect to home
    res.redirect('/');
  });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
