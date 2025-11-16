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

// Secure helper utilities
function escapeHtml(str) {
  if (str === undefined || str === null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function safeCompare(a, b) {
  try {
    const bufA = Buffer.from(String(a));
    const bufB = Buffer.from(String(b));
    if (bufA.length !== bufB.length) return false;
    return crypto.timingSafeEqual(bufA, bufB);
  } catch (e) {
    return false;
  }
}

function isValidDisplayName(name) {
  if (typeof name !== 'string') return false;
  const trimmed = name.trim();
  if (trimmed.length === 0 || trimmed.length > 100) return false;
  // allow letters, numbers, spaces, hyphen, underscore, dot
  return /^[\p{L}\p{N}\s\-\_\.]+$/u.test(trimmed);
}

function isValidEmail(email) {
  if (typeof email !== 'string') return false;
  const trimmed = email.trim();
  if (trimmed.length === 0 || trimmed.length > 254) return false;
  // simple, robust email validation (not exhaustive)
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed);
}

// GET settings - show form (authenticated)
app.get("/settings", ensureLoggedIn, (req, res) => {
  // set safe headers per-route
  res.setHeader('Content-Security-Policy', "default-src 'self'; frame-ancestors 'none'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');

  const current = req.session.userSettings || {};
  const displayName = escapeHtml(current.displayName || '');
  // Do not expose raw session tokens or private data (e.g. CSRF token) in rendered output
  const csrfHidden = `<input type="hidden" name="csrfToken" value="${escapeHtml(req.session.csrfToken)}">`;

  res.send(`<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
  <h1>Account settings</h1>
  <form method="post" action="/settings" autocomplete="off" novalidate>
    ${csrfHidden}
    <label>Display name (max 100 chars):<br>
      <input type="text" name="displayName" value="${displayName}" maxlength="100" required>
    </label><br><br>
    <label>Email (optional):<br>
      <input type="email" name="email" value="${escapeHtml(current.email || '')}" maxlength="254">
    </label><br><br>
    <button type="submit">Save</button>
  </form>
  <p><a href="/">Home</a></p>
</body>
</html>`);
});

// POST settings - validate, sanitize, store (authenticated)
app.post("/settings", ensureLoggedIn, (req, res) => {
  // headers again for POST responses
  res.setHeader('Content-Security-Policy', "default-src 'self'; frame-ancestors 'none'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');

  // Simple CSRF protection: compare token from body against session token safely
  const token = req.body && req.body.csrfToken;
  if (!token || !safeCompare(token, req.session.csrfToken)) {
    return res.status(403).send('Forbidden - invalid CSRF token');
  }

  // enforce maximum sizes to prevent large payloads
  const rawDisplayName = (req.body.displayName || '').toString();
  const rawEmail = (req.body.email || '').toString();

  if (!isValidDisplayName(rawDisplayName)) {
    return res.status(400).send('Invalid display name');
  }
  if (rawEmail && !isValidEmail(rawEmail)) {
    return res.status(400).send('Invalid email address');
  }

  // sanitize values for storage
  const safeDisplayName = rawDisplayName.trim();
  const safeEmail = rawEmail ? rawEmail.trim().toLowerCase() : '';

  // store minimal necessary info in session. Avoid storing secrets or tokens.
  req.session.userSettings = {
    displayName: safeDisplayName,
    email: safeEmail
  };

  // rotate CSRF token to reduce replay risk
  req.session.csrfToken = crypto.randomBytes(24).toString('hex');

  // Redirect to GET to follow PRG pattern and avoid reposts
  return res.redirect('/settings');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
