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
            <h1>User Settings</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>
                New email: <input type="email" name="email" required>
              </p>
              <p>
                New display name: <input type="text" name="displayName" required>
              </p>
              <button type="submit">Save Settings</button>
            </form>
          </body>
        </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const { csrfToken, email, displayName } = req.body;
    if (csrfToken !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    function escapeHtml(str) {
      return str.replace(/[&<>"']/g, c => ({
        '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
      })[c]);
    }

    const safeEmail = escapeHtml(email);
    const safeName = escapeHtml(displayName);
    req.session.userSettings = { email: safeEmail, displayName: safeName };

    res.send(`
        <html>
          <body>
            <h1>Settings Updated</h1>
            <p>Email: ${safeEmail}</p>
            <p>Display Name: ${safeName}</p>
            <a href="/settings">Back to settings</a>
          </body>
        </html>
    `);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
