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
  if (!req.session || !req.session.authenticated || typeof req.session.username !== 'string') {
    return res.redirect("/login");
  }
  next();
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// the login page
app.get("/login", (req, res) => {
  res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" maxlength="32" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
  const rawUsername = typeof req.body.username === 'string' ? req.body.username.trim() : '';
  const password = typeof req.body.password === 'string' ? req.body.password : '';

  if (!/^[A-Za-z0-9_-]{1,32}$/.test(rawUsername) || password.length === 0 || password.length > 128) {
    return res.status(400).send('Invalid input');
  }

  // Replace this check with a proper authentication backend in production.
  const valid = rawUsername === 'admin' && password === 'password';
  if (!valid) {
    return res.status(401).send('Bad username or password');
  }

  req.session.regenerate(function (err) {
    if (err) {
      return res.status(500).send('Server error');
    }
    req.session.authenticated = true;
    req.session.username = rawUsername;
    res.redirect('/settings');
  });
});

// the home page
app.get("/", (req, res) => {
  res.send(`<html>
        Hello! <a href='./login'>Login here</a>
    </html>`);
});

app.get("/settings", ensureLoggedIn, (req, res) => {
  const username = req.session.username;
  if (!/^[A-Za-z0-9_-]{1,32}$/.test(username)) {
    return res.status(400).send('Invalid session');
  }
  const safeUser = escapeHtml(username);
  const csrf = escapeHtml(res.locals.csrfToken || '');
  res.send(`<html>
    <head><meta charset="utf-8"></head>
    <body>
      <h1>Settings</h1>
      <p>User: ${safeUser}</p>
      <form method="post" action="/settings">
        <input type="hidden" name="csrfToken" value="${csrf}">
        <label>Display name: <input name="displayName" maxlength="64"></label>
        <button type="submit">Save</button>
      </form>
    </body>
  </html>`);
});

// handle settings updates with CSRF and input validation
app.post("/settings", ensureLoggedIn, (req, res) => {
  const token = req.body.csrfToken;
  if (!token || token !== req.session.csrfToken) {
    return res.status(403).send('Forbidden');
  }

  const displayName = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
  if (displayName.length > 64) {
    return res.status(400).send('Invalid input');
  }

  // Persisting user settings omitted; this is a placeholder.
  res.send(`<html><body>Saved. <a href="/settings">Back</a></body></html>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
