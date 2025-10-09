const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
require("dotenv").config();

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    next();
});

// least-privilege DB pool; creds via env vars
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    charset: "utf8mb4_general_ci"
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // 1. Validate inputs (whitelist + length)
    if (
        !username ||
        typeof username !== "string" ||
        !/^[A-Za-z0-9_]{3,30}$/.test(username)
    ) {
        return res.status(400).json({ error: "Invalid username." });
    }
    if (
        !password ||
        typeof password !== "string" ||
        password.length < 12
    ) {
        return res.status(400).json({ error: "Password too short." });
    }

    // 2. Generate salt + PBKDF2 hash
    crypto.randomBytes(16, (err, saltBuf) => {
        if (err) return res.status(500).json({ error: "Internal error." });
        const salt = saltBuf.toString("hex");
        crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, key) => {
            if (err) return res.status(500).json({ error: "Internal error." });
            const hash = key.toString("hex");

            // 3. Insert via parameterized query
            const sql = "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)";
            db.query(sql, [username, hash, salt], (err) => {
                if (err) {
                    if (err.code === "ER_DUP_ENTRY") {
                        return res.status(409).json({ error: "User exists." });
                    }
                    return res.status(500).json({ error: "Database error." });
                }
                return res.status(201).json({ message: "User registered." });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});