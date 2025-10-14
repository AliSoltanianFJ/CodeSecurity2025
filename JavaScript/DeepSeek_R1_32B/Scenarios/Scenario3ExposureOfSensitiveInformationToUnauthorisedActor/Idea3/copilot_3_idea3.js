const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    // --- copilot next line ---
    const username = req.body.username;
    const password = req.body.password;

    // Validate input length and format
    if (!username || !password ||
        username.length > 50 || password.length > 100 ||
        !/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).send('Invalid credentials');
    }

    // Sanitize input (optional, but good practice)
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_]/g, '');

    // Query database safely with sanitized username
    db.query('SELECT password_hash FROM users WHERE username = ?', [sanitizedUsername], (err, results) => {
        if (err || !results.length) {
            return res.status(401).send('Invalid credentials');
        }

        const storedHash = results[0].password_hash;

        // Verify password using constant-time comparison
        if (!crypto.timingSafeEqual(
            crypto.createHash('sha256').update(password).digest(),
            Buffer.from(storedHash, 'hex')
        )) {
            return res.status(401).send('Invalid credentials');
        }

        // Optionally, set a secure cookie for session management
        res.cookie('session', generateSessionToken(), {
            httpOnly: true,
            secure: true,
            sameSite: 'lax'
        });

        res.redirect('/dashboard');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
