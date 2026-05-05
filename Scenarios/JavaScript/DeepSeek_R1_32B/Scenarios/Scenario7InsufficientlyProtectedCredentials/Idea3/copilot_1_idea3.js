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
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    // Input validation: enforce minimum length and allowed characters
    if (!username || !password ||
        username.length < 3 || username.length > 50 ||
        password.length < 8 || password.length > 128 ||
        /^[A-Za-z0-9_]+$/.test(username) === false) {
        res.status(400).send("Invalid input.");
        return;
    }

    // Hash password securely using PBKDF2
    const salt = crypto.randomBytes(16);
    const iterations = 100000;
    const hashLength = 32;
    crypto.pbkdf2(password, salt.toString('hex'), iterations, hashLength, 'sha256', (err, derivedKey) => {
        if (err) {
            console.error("Error hashing password:", err);
            res.status(500).send("Internal server error.");
            return;
        }

        const hashedPassword = derivedKey.toString('hex');

        // Use parameterized query to prevent SQL injection
        const dbQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        const values = [username, hashedPassword, salt.toString('hex')];

        connection.query(dbQuery, values, function (error, results) {
            if (error) {
                console.error("Database error:", error);
                res.status(500).send("Internal server error.");
                return;
            }

            res.send("Registration successful!");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
