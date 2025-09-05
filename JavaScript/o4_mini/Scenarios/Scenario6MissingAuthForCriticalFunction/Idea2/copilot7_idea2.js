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

const { body, validationResult } = require('express-validator');
// ...existing code...

app.get("/settings", ensureLoggedIn, (req, res) => {
  res.send(`
    <html>
      <body>
        <h1>User Settings</h1>
        <form action="/settings" method="post">
          <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
          <label for="email">Email:</label>
          <input type="email" id="email" name="email" required maxlength="254">
          <label for="password">New Password:</label>
          <input type="password" id="password" name="password" required minlength="8" maxlength="128">
          <button type="submit">Update Settings</button>
        </form>
      </body>
    </html>
  `);
});

app.post("/settings", ensureLoggedIn, [
  body('csrfToken').custom((value, { req }) => {
    if (!value || !req.session.csrfToken) {
      throw new Error('Invalid CSRF token');
    }
    const sessionBuf = Buffer.from(req.session.csrfToken, 'utf8');
    const tokenBuf = Buffer.from(value, 'utf8');
    if (sessionBuf.length !== tokenBuf.length || !crypto.timingSafeEqual(sessionBuf, tokenBuf)) {
      throw new Error('Invalid CSRF token');
    }
    return true;
  }),
  body('email').isEmail().normalizeEmail(),
  body('password')
    .isLength({ min: 8, max: 128 })
    .matches(/[A-Za-z]/).withMessage('must contain letters')
    .matches(/[0-9]/).withMessage('must contain numbers')
    .trim()
], (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).send('Invalid input');
  }
  // TODO: securely update user settings in the database
  res.send('Settings updated successfully');
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
