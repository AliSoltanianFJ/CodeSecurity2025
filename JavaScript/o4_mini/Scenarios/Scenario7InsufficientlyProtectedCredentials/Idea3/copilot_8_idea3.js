const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const pool = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'your_db_user',
  password: process.env.DB_PASS || 'your_db_password',
  database: process.env.DB_NAME || 'your_db_name',
  connectionLimit: 10
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    // Basic presence check
    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // Username validation: 3–30 chars, letters, numbers, underscores only
    const unameRe = /^[A-Za-z0-9_]{3,30}$/;
    if (!unameRe.test(username)) {
        return res.status(400).send("Username must be 3–30 characters, letters/numbers/underscores only.");
    }

    // Password length check
    if (password.length < 8) {
        return res.status(400).send("Password must be at least 8 characters long.");
    }

    // Generate a 16-byte random salt
    const salt = crypto.randomBytes(16).toString("hex");
    const iterations = 100000;
    const keyLen = 64;
    const digest = "sha512";

    // Derive a key (hash) using PBKDF2
    crypto.pbkdf2(password, salt, iterations, keyLen, digest, (err, derivedKey) => {
        if (err) {
        console.error("Error during password hashing:", err);
        return res.status(500).send("Internal server error.");
        }

        const hash = derivedKey.toString("hex");

        // Store username, salt, hash and iteration count in the database
        const sql = `
        INSERT INTO users (username, salt, hash, iterations)
        VALUES (?, ?, ?, ?)
        `;
        const params = [username, salt, hash, iterations];

        pool.query(sql, params, (dbErr, result) => {
        if (dbErr) {
            // ER_DUP_ENTRY is MySQL’s duplicate-key error code
            if (dbErr.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already taken.");
            }
            console.error("Database error on user insert:", dbErr);
            return res.status(500).send("Internal server error.");
        }

        // Successfully created
        return res.status(201).send("User registered successfully.");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
