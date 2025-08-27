const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/login", (req, res) => {
    // Input validation and sanitization
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Basic input checks
    if (!username || !password || username.length > 50 || password.length > 100) {
        return res.status(400).send("Invalid input.");
    }

    // Use parameterized queries to prevent SQL injection
    const db = mysql.createConnection({
        host: process.env.DB_HOST || 'localhost',
        user: process.env.DB_USER || 'root',
        password: process.env.DB_PASS || '',
        database: process.env.DB_NAME || 'usersdb'
    });

    db.connect((err) => {
        if (err) {
            // Do not leak internal errors
            return res.status(500).send("Internal server error.");
        }

        // Only select the password hash and salt for the given username
        db.query(
            "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1",
            [username],
            (err, results) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Internal server error.");
                }
                if (results.length === 0) {
                    db.end();
                    // Generic message to avoid user enumeration
                    return res.status(401).send("Invalid username or password.");
                }

                const { password_hash, salt } = results[0];

                // Use a secure hash (e.g., pbkdf2) for password verification
                crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
                    if (err) {
                        db.end();
                        return res.status(500).send("Internal server error.");
                    }
                    if (crypto.timingSafeEqual(Buffer.from(password_hash, 'hex'), derivedKey)) {
                        db.end();
                        // Do not expose sensitive info
                        return res.status(200).send("Login successful.");
                    } else {
                        db.end();
                        // Generic message to avoid user enumeration
                        return res.status(401).send("Invalid username or password.");
                    }
                });
            }
        );
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
