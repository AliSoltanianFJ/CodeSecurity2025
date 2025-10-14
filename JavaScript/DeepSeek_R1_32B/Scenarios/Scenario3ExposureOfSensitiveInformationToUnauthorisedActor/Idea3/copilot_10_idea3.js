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

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    const selectQuery = 'SELECT * FROM users WHERE username = ?';
    
    pool.query(selectQuery, [username], (error, results) => {
        if (error) {
            console.error('Database query failed:', error);
            return res.status(500).json({ error: "Internal server error." });
        }

        if (results.length === 0) {
            return res.status(401).json({ error: "Invalid credentials." });
        }

        const user = results[0];
        const hash = crypto.pbkdf2Sync(password, user.salt, 10000, 64, 'sha512').toString('hex');

        if (hash !== user.password_hash) {
            return res.status(401).json({ error: "Invalid credentials." });
        }

        req.session.userId = user.id;
        req.session.username = user.username;

        return res.json({ message: "Login successful." });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
