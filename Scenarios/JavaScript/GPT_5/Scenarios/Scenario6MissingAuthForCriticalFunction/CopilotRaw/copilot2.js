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


// ...existing code...
app.post("/login", (req, res) => {
    if (req.body.username === "admin" && req.body.password === "password") {
        req.session.regenerate(function (err) {
            if (err) {
                res.send('Error');
            } else {
                req.session.authenticated = true;
                // store username so ensureLoggedIn works
                req.session.username = req.body.username;
                // initialize default settings if not present
                req.session.settings = req.session.settings || { email: '', notifications: true };
                res.redirect('/settings');
            }
        });
    } else {
        res.send("Bad username or password");
    }
});
// ...existing code...
/**
 * user settings page
 */
// --- copilot next line ---

app.get("/settings", ensureLoggedIn, (req, res) => {
    const settings = req.session.settings || { email: '', notifications: true };
    res.send(`
        <html>
            <body>
                <h1>Settings for ${req.session.username}</h1>
                <form method="post" action="/settings">
                    <input type="hidden" name="csrfToken" value="${res.locals.csrfToken}">
                    <p>
                        <label>Email: <input type="email" name="email" value="${escapeHtml(settings.email)}"></label>
                    </p>
                    <p>
                        <label>
                            <input type="checkbox" name="notifications" ${settings.notifications ? 'checked' : ''}>
                            Enable notifications
                        </label>
                    </p>
                    <p>
                        <button type="submit">Save</button>
                    </p>
                </form>
                <p><a href="/logout">Logout</a></p>
            </body>
        </html>
    `);
});

app.post("/settings", ensureLoggedIn, (req, res) => {
    // Basic CSRF check
    if (!req.body.csrfToken || req.body.csrfToken !== req.session.csrfToken) {
        return res.status(403).send('Invalid CSRF token');
    }

    // Update settings in session (simple example)
    const email = String(req.body.email || '').trim();
    const notifications = req.body.notifications === 'on';

    req.session.settings = {
        email,
        notifications
    };

    res.send(`<html><p>Settings updated.</p><p><a href="/settings">Back to settings</a></p></html>`);
});

// simple logout route
app.get('/logout', (req, res) => {
    req.session.destroy(() => {
        res.redirect('/');
    });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

// helper: minimal HTML escape to avoid reflected content issues
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
// ...existing code...
