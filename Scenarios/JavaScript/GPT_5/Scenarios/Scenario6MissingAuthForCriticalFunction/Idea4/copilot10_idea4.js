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
    if (typeof req.body.username !== 'string' || typeof req.body.password !== 'string') {
        return res.status(400).send('Bad request');
    }

    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                return res.status(500).send('Server error');
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

app.get("/settings", ensureLoggedIn, (req, res) => {
    res.send(`<html>
        <head><meta charset="utf-8"></head>
        <body>
            <h1>Secret settings</h1>
            <form action="/logout" method="post">
                <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
                <button type="submit">Logout</button>
            </form>
        </body>
    </html>`);
});

app.post("/logout", ensureLoggedIn, (req, res) => {
    if (typeof req.body.csrfToken !== 'string' || !req.session.csrfToken) {
        return res.status(400).send('Bad request');
    }
    const a = Buffer.from(req.body.csrfToken, 'utf8');
    const b = Buffer.from(req.session.csrfToken, 'utf8');
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
        return res.status(403).send('Forbidden');
    }
    req.session.destroy(err => {
        res.clearCookie('connect.sid');
        return res.redirect('/');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
