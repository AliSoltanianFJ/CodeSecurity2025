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

    // Username validation: only allow alphanumeric, 3-32 chars
    if (!/^[a-zA-Z0-9]{3,32}$/.test(username)) {
        // Do not leak details about validation failure
        return res.status(400).send("Invalid input.");
    }

    // Password validation: min 12 chars, at least one uppercase, one lowercase, one digit, one special char
    if (!/^.{12,}$/.test(password) ||
        !/[A-Z]/.test(password) ||
        !/[a-z]/.test(password) ||
        !/[0-9]/.test(password) ||
        !/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
        return res.status(400).send("Invalid input.");
    }

    // Security: Use strong password hashing (argon2 recommended, fallback to bcrypt)
    // Avoid using crypto for password hashing (CWE-327)
    const argon2 = require('argon2'); // Secure library for password hashing

    let hashedPassword;
    try {
        // Security: Use secure defaults for argon2
        hashedPassword = await argon2.hash(password, {
            type: argon2.argon2id,
            memoryCost: 2 ** 16, // 64 MB
            timeCost: 4,
            parallelism: 2,
        });
    } catch (err) {
        // Do not leak error details
        return res.status(500).send("Internal error.");
    }

    // Security: Use environment variables for DB credentials, never hardcode
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        multipleStatements: false, // Principle of least privilege
        ssl: { rejectUnauthorized: true } // Ensure encrypted connection if supported
    });

    db.connect((err) => {
        if (err) {
            // Do not leak error details
            return res.status(500).send("Internal error.");
        }

        // Security: Use parameterized queries to prevent SQL injection (CWE-89)
        const query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        db.query(query, [username, hashedPassword], (err, results) => {
            db.end(); // Principle of least privilege: close connection ASAP
            if (err) {
                // Do not leak error details (e.g., duplicate username)
                return res.status(400).send("Registration failed.");
            }
            // Success: Do not leak sensitive info
            return res.status(201).send("Registration successful.");
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
