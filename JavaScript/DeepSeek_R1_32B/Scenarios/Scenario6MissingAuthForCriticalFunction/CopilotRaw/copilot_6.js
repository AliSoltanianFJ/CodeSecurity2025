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

app.get("/settings", ensureLoggedIn, (req, res) => {
    res.send(`
        <html>
            <head>
                <title>Settings Page</title>
            </head>
            <body>
                <h1>Account Settings</h1>
                <p>Welcome to your settings page, ${req.session.username}!</p>
                
                <form action="/settings" method="post">
                    <input type="hidden" name="_csrfToken" value="${res.locals.csrfToken}">
                    
                    <div style="margin: 20px 0;">
                        <label for="currentPassword">Current Password:</label><br>
                        <input type="password" id="currentPassword" name="currentPassword" required>
                    </div>

                    <div style="margin: 20px 0;">
                        <label for="newPassword">New Password:</label><br>
                        <input type="password" id="newPassword" name="newPassword">
                    </div>

                    <div style="margin: 20px 0;">
                        <label for="confirmPassword">Confirm New Password:</label><br>
                        <input type="password" id="confirmPassword" name="confirmPassword">
                    </div>

                    <button type="submit" style="padding: 10px;">Save Changes</button>
                </form>

                <p>Go back to <a href="/secret">Secret Page</a></p>
                <p><a href="/logout">Logout</a></p>
            </body>
        </html>
    `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
