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
    // Validate and sanitize input
    const username = req.body.username?.trim();
    const password = req.body.password?.trim();

    if (!username || !password || username.length > 50 || password.length > 50) {
        return res.status(400).send('Invalid input');
    }

    // Use parameterized query to prevent SQL injection
    const selectUserQuery = 'SELECT * FROM users WHERE username = ?';
    db.query(selectUserQuery, [username], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).send('Internal server error');
        }

        const user = results[0];
        if (!user) {
            return res.status(401).send('Invalid credentials');
        }

        // Verify password hash using bcrypt
        const isValidPassword = user.passwordHash === crypto.pbkdf2Sync(password, user.salt, 10000, 64, 'sha512').toString('hex');

        if (!isValidPassword) {
            return res.status(401).send('Invalid credentials');
        }

        // Set session or JWT token
        req.session.userId = user.id;
        return res.redirect('/');
    });
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
