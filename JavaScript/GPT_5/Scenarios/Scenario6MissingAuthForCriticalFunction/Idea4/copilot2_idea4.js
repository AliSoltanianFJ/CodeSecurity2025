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
    if (!req.session || req.session.authenticated !== true) {
        return res.redirect("/login");
    }
    next();
}

// the login page
app.get("/login", (req, res) => {
    res.send(`
        <html><form action="/login" method="post">
            <p><input type="text" name="username" required maxlength="64"></p>
            <p><input type="password" name="password" required maxlength="128"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `);
});

// the login POST system
app.post("/login", (req, res) => {
    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                res.status(500).send('Internal error');
            } else {
                req.session.authenticated = true;
                res.redirect('/secret');
            }
        });
    } else {
        res.status(401).send("Bad username or password");
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

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

app.get("/settings", ensureLoggedIn, (req, res) => {
    const displayName = req.session.displayName ? escapeHtml(req.session.displayName) : '';
    res.send(`<html>
        <body>
          <h1>Settings</h1>
          <form method="post" action="/settings">
            <label>Display name
              <input type="text" name="displayName" value="${displayName}" maxlength="64" required>
            </label>
            <input type="hidden" name="csrfToken" value="${escapeHtml(res.locals.csrfToken)}">
            <button type="submit">Save</button>
          </form>
        </body>
    </html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const token = req.body.csrfToken;
    const sessionToken = req.session.csrfToken;
    if (!token || !sessionToken) return res.status(400).send('Bad request');
    const a = Buffer.from(String(token));
    const b = Buffer.from(String(sessionToken));
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
        return res.status(403).send('Forbidden');
    }

    const value = typeof req.body.displayName === 'string' ? req.body.displayName.trim() : '';
    if (value.length < 1 || value.length > 64) {
        return res.status(400).send('Invalid input');
    }
    if (!/^[\p{L}\p{N}\s\-_]+$/u.test(value)) {
        return res.status(400).send('Invalid input');
    }

    req.session.displayName = value;
    res.send('<html><body>Settings updated</body></html>');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
