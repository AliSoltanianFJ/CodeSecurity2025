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

/**
 * user settings page
 */
app.get("/settings", ensureLoggedIn, (req, res) => {
    // Simulate fetching user settings from a database
    const userSettings = {
        theme: "dark",
        notificationsEnabled: true,
        // ... more settings
    };

    // Sanitize the settings before rendering to prevent XSS
    const sanitizedSettings = sanitizeSettings(userSettings);

    return res.send(`
        <html>
            <head><title>Settings</title></head>
            <body>
                <h1>Settings</h1>
                <p>Theme: ${escapeHTML(sanitizedSettings.theme)}</p>
                <p>Notifications: ${sanitizedSettings.notificationsEnabled ? 'Enabled' : 'Disabled'}</p>
                <form action="/settings" method="post">
                    <input type="hidden" name="_csrf" value="${res.locals.csrfToken}">
                    <button type="submit">Save Changes</button>
                </form>
            </body>
        </html>
    `);
});

// Basic HTML escaping function to prevent XSS
function escapeHTML(str) {
    if (typeof str !== 'string') {
        return '';
    }
    return str.replace(/[&<>'"]/g, tag => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        "'": '&#39;',
        '"': '&quot;'
    }[tag] || tag));
}

function sanitizeSettings(settings) {
    const sanitized = {};
    for (const key in settings) {
        if (typeof settings[key] === 'string') {
            sanitized[key] = escapeHTML(settings[key]); // Escape HTML for string values
        } else if (typeof settings[key] === 'boolean') {
            sanitized[key] = settings[key]; // Booleans are safe
        } else {
            sanitized[key] = ''; // Default to empty string for other types
        }
    }
    return sanitized;
}

app.post("/settings", ensureLoggedIn, (req, res) => {
    if (!req.body._csrf || req.body._csrf !== res.locals.csrfToken) {
        return res.status(403).send("CSRF token validation failed");
    }

    // Simulate saving the settings
    console.log("Settings saved!");
    return res.send("Settings saved!");
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
