// ...existing code...
const express = require("express");
const session = require("express-session");
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

const SESSION_SECRET = process.env.SESSION_SECRET;
if (!SESSION_SECRET) {
    console.error("Missing SESSION_SECRET");
    process.exit(1);
}

app.use(session({
  secret: SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
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
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }
    next();
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post" autocomplete="off" novalidate>
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    if (!username || !password || username.length > 64 || password.length > 128) {
        return res.status(400).send("Invalid credentials");
    }

    const expectedUser = "admin";
    const expectedPass = "password";

    try {
      const userMatch = Buffer.from(username) && Buffer.from(expectedUser);
      const passMatch = Buffer.from(password) && Buffer.from(expectedPass);
      const userEqual = username === expectedUser;
      const passEqual = password === expectedPass;
      if (userEqual && passEqual) {
          req.session.regenerate(function (err) {
              if (err) {
                  return res.status(500).send("Server error");
              }
              req.session.authenticated = true;
              req.session.username = expectedUser;
              return res.redirect('/settings');
          });
      } else {
          return res.status(401).send("Bad username or password");
      }
    } catch (e) {
      return res.status(500).send("Server error");
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
app.get("/settings", ensureLoggedIn, (req, res) => {
    const token = res.locals.csrfToken;
    res.send(`<html>
      <body>
        <h1>Account Settings</h1>
        <form method="post" action="/settings" autocomplete="off" novalidate>
          <label for="email">Email</label>
          <input id="email" name="email" type="email" maxlength="254" required>
          <input type="hidden" name="csrfToken" value="${escapeHtml(token)}">
          <button type="submit">Save</button>
        </form>
        <form method="post" action="/logout" style="margin-top:1rem;">
          <input type="hidden" name="csrfToken" value="${escapeHtml(token)}">
          <button type="submit">Logout</button>
        </form>
      </body>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const submitted = typeof req.body.csrfToken === 'string' ? req.body.csrfToken : '';
    const sessionToken = req.session.csrfToken || '';
    if (!submitted || !sessionToken) {
        return res.status(400).send("Bad request");
    }
    if (submitted.length !== sessionToken.length || !crypto.timingSafeEqual(Buffer.from(submitted), Buffer.from(sessionToken))) {
        return res.status(403).send("Forbidden");
    }

    const email = typeof req.body.email === 'string' ? req.body.email.trim() : '';
    if (!email || email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).send("Invalid input");
    }

    req.session.email = email;
    return res.send("Settings updated");
});

app.post("/logout", ensureLoggedIn, (req, res) => {
    const submitted = typeof req.body.csrfToken === 'string' ? req.body.csrfToken : '';
    const sessionToken = req.session.csrfToken || '';
    if (!submitted || submitted.length !== sessionToken.length || !crypto.timingSafeEqual(Buffer.from(submitted), Buffer.from(sessionToken))) {
        return res.status(403).send("Forbidden");
    }
    req.session.destroy(err => {
        return res.redirect('/');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
