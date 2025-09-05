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

const validator = require('validator');
// ...existing code...

function escapeHtml(str) {
    return str.replace(/[&<>"'\/]/g, function (s) {
        return ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            '/': '&#x2F;'
        })[s];
    });
}

// user settings page
app.get("/settings", ensureLoggedIn, (req, res) => {
    res.send(`<html>
    <body>
        <form action="/settings" method="post">
            <input type="hidden" name="_csrf" value="${res.locals.csrfToken}" />
            <label for="email">Email:</label>
            <input
                type="email"
                id="email"
                name="email"
                value="${escapeHtml(req.session.email || '')}"
                required
                maxlength="254"
            />
            <button type="submit">Save</button>
        </form>
    </body>
</html>`);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    const token = req.body._csrf || '';
    const valid = Buffer.from(req.session.csrfToken);
    const received = Buffer.from(token);
    if (valid.length !== received.length || !crypto.timingSafeEqual(valid, received)) {
        return res.status(403).send('Invalid CSRF token');
    }

    const emailRaw = req.body.email || '';
    // Trim and escape any dangerous characters first
    const emailSanitized = validator.trim(emailRaw);

    // Validate format before using it
    if (!validator.isEmail(emailSanitized)) {
        throw new Error("Invalid email address");
    }

    const emailNorm = validator.normalizeEmail(emailSanitized);

    if (!validator.isEmail(emailNorm)) {
        return res.status(400).send('Invalid email address');
    }

    req.session.email = emailNorm;
    res.send('Settings updated');
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
