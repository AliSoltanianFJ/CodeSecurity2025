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

// ...existing code...
app.get("/settings", ensureLoggedIn, (req, res) => {
    res.send(`
        <html>
          <body>
            <h1>Account settings</h1>
            <p>Signed in as ${req.session.username || 'unknown'}</p>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>Email: <input type="email" name="email" value="${(req.session.settings && req.session.settings.email) || ''}" /></p>
              <p><button type="submit">Save</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const token = req.body.csrfToken;
    if (!token || token !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    const email = typeof req.body.email === 'string' ? req.body.email.trim() : '';
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).send('Invalid email address');
    }

    req.session.settings = req.session.settings || {};
    req.session.settings.email = email;
    res.send('<html>Settings updated. <a href="/settings">Back to settings</a></html>');
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

