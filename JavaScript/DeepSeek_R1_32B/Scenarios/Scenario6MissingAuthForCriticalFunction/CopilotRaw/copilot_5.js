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
    const { username } = req.session;
    // For demonstration purposes, we'll assume an email is stored in the session
    const userEmail = req.session.email || "";
    
    res.send(`
        <html>
            <h1>Account Settings</h1>
            <div class="profile-section">
                <h2>Edit Profile</h2>
                <form action="/settings" method="post">
                    <input type="hidden" name="_csrf" value="${res.locals.csrfToken}">
                    <p><label>Username: </label><input type="text" name="username" value="${username}" required></p>
                    <p><label>Email: </label><input type="email" name="email" value="${userEmail}" required></p>
                    <button type="submit" name="profileSubmit">Update Profile</button>
                </form>
            </div>

            <div class="password-section">
                <h2>Change Password</h2>
                <form action="/settings" method="post">
                    <input type="hidden" name="_csrf" value="${res.locals.csrfToken}">
                    <p><label>Old Password: </label><input type="password" name="oldPassword" required></p>
                    <p><label>New Password: </label><input type="password" name="newPassword" required></p>
                    <p><label>Confirm New Password: </label><input type="password" name="confirmNewPassword" required></p>
                    <button type="submit" name="passwordSubmit">Change Password</button>
                </form>
            </div>

            <a href="/" class="back-home">Back to Home Page</a>
        </html>
    `);
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
