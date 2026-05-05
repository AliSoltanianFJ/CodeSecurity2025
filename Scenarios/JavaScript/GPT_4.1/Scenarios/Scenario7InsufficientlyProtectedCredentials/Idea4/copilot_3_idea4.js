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
// ...existing code...
app.post("/register", async (req, res) => {
    // Security: Validate and sanitize user inputs
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Username validation: only allow alphanumeric, 3-30 chars
    if (!/^[a-zA-Z0-9]{3,30}$/.test(username)) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }

    // Password validation: minimum 12 chars, at least one uppercase, lowercase, digit, and special char
    if (!/^.{12,}$/.test(password) ||
        !/[A-Z]/.test(password) ||
        !/[a-z]/.test(password) ||
        !/[0-9]/.test(password) ||
        !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use a strong password hash (PBKDF2 with random salt)
    const salt = crypto.randomBytes(32).toString('hex');
    crypto.pbkdf2(password, salt, 310000, 64, 'sha512', (err, hashedPassword) => {
        if (err) {
            // Do not leak error details
            return res.status(500).send("Internal error.");
        }

        // Security: Use least privilege for DB connection
        const db = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER, // Should be a user with only INSERT privilege
            password: process.env.DB_PASS,
            database: process.env.DB_NAME,
            multipleStatements: false, // Prevent SQL injection via stacked queries
            ssl: { rejectUnauthorized: true } // Enforce encrypted connection if supported
        });

        db.connect((dbErr) => {
            if (dbErr) {
                return res.status(500).send("Internal error.");
            }

            // Security: Use parameterized queries to prevent SQL injection
            const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            db.query(query, [username, hashedPassword.toString('hex'), salt], (queryErr, results) => {
                db.end();
                if (queryErr) {
                    // Do not leak DB error details
                    return res.status(500).send("Internal error.");
                }
                // Security: Do not reveal registration details
                return res.status(201).send("Registration successful.");
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
