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

// a small helper to escape any values you interpolate into your HTML
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// GET /settings — show the settings form
app.get(
  "/settings",
  ensureLoggedIn,
  (req, res) => {
    // (If you have a real user store, you would pull their current settings out of the DB here.
    //  For this example we’ll pretend req.session.user contains the current user data.)
    const currentEmail = req.session.user && req.session.user.email
      ? req.session.user.email
      : "";

    res.send(`
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>User Settings</title>
</head>
<body>
  <h1>Account Settings</h1>
  <form method="post" action="/settings">
    <!-- CSRF token, injected into the template above -->
    <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">

    <p>
      <label for="email">Email:</label><br>
      <input
        type="email"
        id="email"
        name="email"
        value="${escapeHtml(currentEmail)}"
        required
      >
    </p>

    <p>
      <button type="submit">Save changes</button>
    </p>
  </form>
</body>
</html>
    `);
  }
);

// POST /settings — process the form
app.post(
  "/settings",
  ensureLoggedIn,
  (req, res) => {
    // 1. Verify the CSRF token
    const sentToken = req.body.csrfToken;
    if (!sentToken || sentToken !== req.session.csrfToken) {
      return res.status(403).send("Invalid CSRF token");
    }

    // 2. Extract, validate & sanitize inputs
    let email = (req.body.email || "").trim();
    email = validator.normalizeEmail(email, { all_lowercase: true });
    if (!email || !validator.isEmail(email)) {
      return res.status(400).send("Please provide a valid email address.");
    }

    // 3. Persist the change
    //    In a real app you'd update your database here.
    //    For this example we'll just stash it in the session.
    req.session.user = req.session.user || {};
    req.session.user.email = email;

    // 4. Rotate the CSRF token so that a stolen form can't be replayed
    req.session.csrfToken = crypto.randomBytes(24).toString("hex");

    // 5. Respond
    res.send(`
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Settings Updated</title>
</head>
<body>
  <h1>Settings Updated</h1>
  <p>Your email has been changed to <strong>${escapeHtml(email)}</strong>.</p>
  <p><a href="/settings">Back to settings</a></p>
</body>
</html>
    `);
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
