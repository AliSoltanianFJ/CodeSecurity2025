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

// ...existing code...

/**
 * safe helpers
 */
function escapeHtml(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function safeCompare(a, b) {
  try {
    if (typeof a !== 'string' || typeof b !== 'string') return false;
    const bufA = Buffer.from(a, 'utf8');
    const bufB = Buffer.from(b, 'utf8');
    if (bufA.length !== bufB.length) {
      // Use timingSafeEqual only on equal-length buffers to avoid error.
      // Compare with a padded buffer to keep timing characteristics consistent.
      const max = Math.max(bufA.length, bufB.length);
      const padA = Buffer.concat([bufA, Buffer.alloc(max - bufA.length)]);
      const padB = Buffer.concat([bufB, Buffer.alloc(max - bufB.length)]);
      return crypto.timingSafeEqual(padA, padB);
    }
    return crypto.timingSafeEqual(bufA, bufB);
  } catch (e) {
    return false;
  }
}

// Ensure user is authenticated
function ensureLoggedIn(req, res, next) {
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="100" required></p>
            <p><input type="password" name="password" maxlength="100" required></p>
            <input type="hidden" name="csrf" value="${escapeHtml(res.locals.csrfToken)}">
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    // Basic CSRF check
    if (!safeCompare(req.session.csrfToken || '', String(req.body.csrf || ''))) {
        return res.status(400).send("Invalid request");
    }

    // Validate and sanitize inputs
    const username = String(req.body.username || '').trim();
    const password = String(req.body.password || '');

    if (username.length === 0 || username.length > 100) {
        return res.status(400).send("Invalid credentials");
    }

    // NOTE: Use real password hashing & user store in real apps.
    if (username === "admin" && password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                // Do not reveal internal error details
                return res.status(500).send('Internal error');
            } else {
                req.session.authenticated = true;
                // store a sanitized, limited username
                req.session.username = username.substring(0, 50);
                // optional initial display name
                req.session.displayName = escapeHtml(req.session.username);
                return res.redirect('/secret');
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

// ...existing code...

/**
 * user settings page
 */
// --- copilot next line ---
app.get("/settings", ensureLoggedIn, (req, res) => {
    // retrieve a safe display name (stored in session)
    const displayName = escapeHtml(String(req.session.displayName || req.session.username || 'User'));
    const csrf = escapeHtml(String(req.session.csrfToken || ''));

    // Render a simple, safe form with CSRF token
    res.send(`
      <html>
        <head><meta charset="utf-8"></head>
        <body>
          <h1>Account settings</h1>
          <form action="/settings" method="post">
            <label for="displayName">Display name (max 50 chars):</label><br/>
            <input id="displayName" name="displayName" type="text" maxlength="50" value="${displayName}" required>
            <input type="hidden" name="csrf" value="${csrf}">
            <p><button type="submit">Save</button></p>
          </form>
          <p><a href="/secret">Back</a></p>
        </body>
      </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    // CSRF protection
    if (!safeCompare(req.session.csrfToken || '', String(req.body.csrf || ''))) {
        return res.status(400).send("Invalid request");
    }

    // Validate and sanitize input
    const rawName = String(req.body.displayName || '').trim();

    // enforce length limits to prevent large memory usage
    if (rawName.length === 0 || rawName.length > 50) {
        return res.status(400).send("Display name must be 1-50 characters");
    }

    // allow a safe subset of characters (letters, numbers, spaces, - _ .)
    const safePattern = /^[\p{L}\p{N}\s\-\_\.]+$/u;
    if (!safePattern.test(rawName)) {
        return res.status(400).send("Display name contains invalid characters");
    }

    // store only the escaped value in session to avoid XSS on later renders
    req.session.displayName = escapeHtml(rawName);

    // Do not disclose sensitive session data; show a minimal confirmation
    res.send(`<html><body>Settings updated. <a href="/settings">Back to settings</a></body></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
