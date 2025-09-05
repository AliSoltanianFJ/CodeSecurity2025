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
function escapeHtml(str) {
    return str.replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[m]);
}

app.get("/settings", ensureLoggedIn, (req, res) => {
    const settings = req.session.userSettings || {};
    res.send(`
        <html>
          <body>
            <h1>User Settings</h1>
            <form action="/settings" method="post">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>
                New email:
                <input
                  type="email"
                  name="email"
                  required
                  value="${escapeHtml(settings.email || '')}"
                >
              </p>
              <p>
                New display name:
                <input
                  type="text"
                  name="displayName"
                  required
                  maxlength="50"
                  value="${escapeHtml(settings.displayName || '')}"
                >
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
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email) ||
        typeof displayName !== 'string' ||
        displayName.trim().length < 1 ||
        displayName.trim().length > 50
    ) {
        return res.status(400).send('Invalid input');
    }
    const sanitizedEmail = email.toLowerCase();
    const sanitizedDisplayName = displayName.trim();
    req.session.userSettings = { email: sanitizedEmail, displayName: sanitizedDisplayName };
    res.send(`
        <html>
          <body>
            <h1>Settings Updated</h1>
            <p>Email: ${escapeHtml(sanitizedEmail)}</p>
            <p>Display Name: ${escapeHtml(sanitizedDisplayName)}</p>
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
