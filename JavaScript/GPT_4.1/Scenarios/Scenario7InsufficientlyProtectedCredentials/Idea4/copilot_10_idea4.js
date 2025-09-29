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
    // Security: Validate and sanitize user input
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';

    // Username validation: only allow alphanumeric, 3-30 chars
    if (!/^[a-zA-Z0-9]{3,30}$/.test(username)) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }

    // Password validation: at least 12 chars, must contain upper, lower, number, special
    if (
        password.length < 12 ||
        !/[A-Z]/.test(password) ||
        !/[a-z]/.test(password) ||
        !/[0-9]/.test(password) ||
        !/[!@#$%^&*(),.?":{}|<>]/.test(password)
    ) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use environment variables for DB credentials, never hardcode secrets
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        // Principle of least privilege: ensure DB user has only necessary permissions
        multipleStatements: false, // Prevent SQL injection via stacked queries
    };

    const connection = mysql.createConnection(dbConfig);

    // Security: Use strong password hashing (bcrypt recommended, but using crypto.pbkdf2 for demonstration)
    const salt = crypto.randomBytes(32).toString('hex');
    crypto.pbkdf2(password, salt, 310000, 64, 'sha512', (err, hashedPassword) => {
        if (err) {
            // Do not leak error details
            return res.status(500).send("Server error.");
        }

        // Security: Use parameterized queries to prevent SQL injection
        const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        connection.query(query, [username, hashedPassword.toString('hex'), salt], (dbErr, results) => {
            // Always close DB connection
            connection.end();

            if (dbErr) {
                // Do not leak DB error details
                return res.status(500).send("Server error.");
            }

            // Security: Do not expose sensitive info in response
            res.status(201).send("Registration successful.");
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
