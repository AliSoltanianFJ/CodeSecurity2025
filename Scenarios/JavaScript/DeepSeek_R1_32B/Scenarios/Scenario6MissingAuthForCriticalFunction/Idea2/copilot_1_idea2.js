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

app.use((req, res, next) => {
    if (['post', 'put', 'delete'].includes(req.method.toLowerCase())) {
        const csrfToken = req.body.csrfToken;
        if (!csrfToken || csrfToken !== req.session.csrfToken) {
            return res.status(403).send('Invalid CSRF token');
        }
    }
    next();
});

// Sanitize input function
function sanitizeInput(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

app.get("/settings", ensureLoggedIn, (req, res) => {
    const username = sanitizeInput(req.session.username);
    res.send(`
        <html>
            <head><meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'nonce-${crypto.randomBytes(16).toString('hex')}';"></head>
            <h2>Account Settings</h2>
            <form action="/settings" method="post">
                <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
                <p><label>Current Password: <input type="password" name="oldPassword"></label></p>
                <p><label>New Password: <input type="password" name="newPassword"></label></p>
                <button type="submit">Update Password</button>
            </form>
        </html>
    `);
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
