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

// simple HTML-escape helper
function escapeHTML(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * User settings page
 */
// GET /settings — show form
app.get("/settings", ensureLoggedIn, (req, res) => {
  // If you already store some settings (e.g. displayName), pull them here.
  const currentName = req.session.displayName || "";

  res.send(`
    <html>
      <head><title>Your Settings</title></head>
      <body>
        <h1>Account Settings</h1>
        <form action="/settings" method="POST">
          <label>
            Display Name:
            <input
              type="text"
              name="displayName"
              value="${escapeHTML(currentName)}"
              maxlength="50"
              required
            />
          </label>
          
          <!-- CSRF token -->
          <input
            type="hidden"
            name="csrfToken"
            value="${res.locals.csrfToken}"
          />
          
          <p>
            <button type="submit">Save Settings</button>
          </p>
        </form>
        <p><a href="/">Back to home</a></p>
      </body>
    </html>
  `);
});

// POST /settings — process updates
app.post("/settings", ensureLoggedIn, (req, res) => {
  // 1. CSRF check
  if (!req.body.csrfToken || req.body.csrfToken !== req.session.csrfToken) {
    return res.status(403).send("Invalid CSRF token");
  }

  // 2. Basic validation
  let newName = req.body.displayName;
  if (typeof newName !== "string") {
    return res.status(400).send("Invalid display name");
  }
  newName = newName.trim();
  if (newName.length === 0 || newName.length > 50) {
    return res.status(400).send("Display name must be 1–50 characters");
  }
  // Only allow letters, numbers, spaces, dashes and underscores:
  if (!/^[\w \-]+$/.test(newName)) {
    return res.status(400).send("Display name contains invalid characters");
  }

  // 3. “Store” the setting (in this example, in session; replace with DB in real apps)
  req.session.displayName = newName;

  // 4. Feedback to user
  res.send(`
    <html>
      <head><title>Settings Updated</title></head>
      <body>
        <p>Your settings have been updated.</p>
        <p><a href="/settings">Back to settings</a></p>
      </body>
    </html>
  `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
