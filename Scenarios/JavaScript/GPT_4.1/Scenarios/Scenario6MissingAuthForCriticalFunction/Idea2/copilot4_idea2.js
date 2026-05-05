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


app.post("/settings", ensureLoggedIn, (req, res) => {
    // CSRF protection
    if (req.body.csrfToken !== req.session.csrfToken) {
        return res.status(403).send("Invalid CSRF token");
    }

    // Input validation and sanitization
    const displayName = (req.body.displayName || "").trim();
    if (
        typeof displayName !== "string" ||
        displayName.length === 0 ||
        displayName.length > 50 ||
        !/^[a-zA-Z0-9 _-]+$/.test(displayName)
    ) {
        return res.status(400).send("Invalid display name");
    }

    // Simulate saving the display name securely (no deserialization, no sensitive info)
    // In a real app, update the user's profile in the database here

    res.send(`
        <html>
            <head>
                <meta http-equiv="Content-Security-Policy" content="default-src 'self'">
            </head>
            <body>
                <h2>Settings updated</h2>
                <p>Your display name is now: ${displayName.replace(/[<>&"'`]/g, '')}</p>
                <a href="/settings">Back to settings</a>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
