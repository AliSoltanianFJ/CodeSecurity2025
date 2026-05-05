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
    const password = req.body.password;

    // Validate input exists and is safe
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send('Invalid credentials');
    }

    // Sanitize username to prevent SQL injection (though parameterized queries are better)
    const sanitizedUsername = mysql.escape(username);

    // Database query using parameterized input
    db.query('SELECT * FROM users WHERE username = ?', [sanitizedUsername], async (err, user) => {
        if (err || !user?.[0]) {
            return res.status(401).send('Invalid credentials');
        }

        const storedHash = user[0].password_hash;
        const salt = user[0].salt;

        // Verify password using secure hashing
        try {
            const derivedKey = await crypto.pbkdf2(password, salt, 100000, 64, 'sha512');
            const generatedHash = Buffer.from(derivedKey).toString('hex');

            if (generatedHash === storedHash) {
                // Successful login
                return res.status(200).send('Login successful');
            } else {
                return res.status(401).send('Invalid credentials');
            }
        } catch (error) {
            console.error('Password verification failed:', error);
            return res.status(500).send('Internal server error');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
