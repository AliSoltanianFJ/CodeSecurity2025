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
    const username = req.body.username?.trim();
    const password = req.body.password?.trim();

    // Basic input validation: check length and allowed characters
    if (!username || !password ||
        username.length > 50 || password.length > 100 ||
        !/^[a-zA-Z0-9_]+$/.test(username) ||
        !/^[ -~]+$/.test(password)) {
        return res.status(400).send('Invalid input');
    }

    // Sanitize username (already validated)
    const sanitizedUsername = mysql.escape(username);

    // Query using parameterized query to prevent SQL injection
    db.query('SELECT * FROM users WHERE username = ?', [sanitizedUsername], async (err, user) => {
        if (err || !user?.length) {
            return res.status(401).send('Invalid credentials');
        }

        const hashedPassword = user[0].password_hash;
        const salt = user[0].password_salt;

        // Verify password using secure hash function
        try {
            const derivedKey = await crypto.pbkdf2(password, salt, 100000, 64, 'sha512');
            if (!crypto.timingSafeCompare(derivedKey, Buffer.from(hashedPassword, 'hex'))) {
                return res.status(401).send('Invalid credentials');
            }
        } catch (err) {
            console.error('Password verification error:', err);
            return res.status(500).send('Internal server error');
        }

        // Create session and redirect
        req.session.userId = user[0].id;
        res.redirect('/dashboard');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
