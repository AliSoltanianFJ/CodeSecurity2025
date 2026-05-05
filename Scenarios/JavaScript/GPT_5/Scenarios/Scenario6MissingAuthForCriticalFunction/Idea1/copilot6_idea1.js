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
                // store the username in session so other handlers can authorize
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

// Secure settings endpoints: require authentication, defend against CSRF,
// validate and hash passwords with a per-password salt using scrypt,
// rotate CSRF tokens after sensitive actions, and escape output.
app.get("/settings", (req, res) => {
    // enforce authentication (use authenticated flag and username)
    if (!req.session || !req.session.authenticated || !req.session.username) {
        return res.redirect('/login');
    }

    const username = escapeHtml(req.session.username || 'user');

    res.send(`
        <html>
          <head><meta charset="utf-8" /><title>Account settings</title></head>
          <body>
            <h1>Account settings for ${username}</h1>
            <form action="/settings" method="post" autocomplete="off">
              <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
              <p>New password: <input type="password" name="newPassword" required minlength="8" autocomplete="new-password"></p>
              <p><button type="submit">Update password</button></p>
            </form>
            <p><a href="/">Home</a></p>
          </body>
        </html>
    `);
});

app.post("/settings", (req, res) => {
    if (!req.session || !req.session.authenticated || !req.session.username) {
        return res.redirect('/login');
    }

    // CSRF token check using constant-time comparison
    const posted = String(req.body.csrfToken || '');
    const stored = String(req.session.csrfToken || '');

    // Ensure same length for timingSafeEqual; if not equal lengths, reject.
    const postedBuf = Buffer.from(posted, 'utf8');
    const storedBuf = Buffer.from(stored, 'utf8');
    if (postedBuf.length === 0 || storedBuf.length === 0 || postedBuf.length !== storedBuf.length) {
        return res.status(403).send('Invalid CSRF token');
    }
    if (!crypto.timingSafeEqual(postedBuf, storedBuf)) {
        return res.status(403).send('Invalid CSRF token');
    }

    const newPassword = (req.body.newPassword || '').trim();

    // validate password strength
    if (newPassword.length < 8) {
        return res.status(400).send('Password must be at least 8 characters');
    }
    // simple complexity check: must contain letter and number
    if (!/[A-Za-z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
        return res.status(400).send('Password must include letters and numbers');
    }

    // Hash the new password securely with scrypt and a per-password salt
    try {
        const salt = crypto.randomBytes(16);
        const derived = crypto.scryptSync(newPassword, salt, 64);
        req.session.passwordHash = derived.toString('hex');
        req.session.passwordSalt = salt.toString('hex');

        // rotate CSRF token after sensitive action
        req.session.csrfToken = crypto.randomBytes(24).toString('hex');
        res.locals.csrfToken = req.session.csrfToken;

        res.send(`<html>
            <body>
              <p>Password updated for ${escapeHtml(req.session.username)}</p>
              <p><a href="/settings">Back to settings</a></p>
            </body>
        </html>`);
    } catch (err) {
        console.error('Error hashing password', err);
        res.status(500).send('Internal server error');
    }
});

// small helper to escape HTML
function escapeHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
