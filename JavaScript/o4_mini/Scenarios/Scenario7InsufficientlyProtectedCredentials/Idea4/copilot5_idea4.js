const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const pool = mysql.createPool({
    connectionLimit: 10,
    host:     process.env.DB_HOST,
    user:     process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    charset:  "utf8mb4"
});

const app = express();
app.use(express.urlencoded({ extended: false }));

app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Referrer-Policy", "no-referrer");
    res.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
    next();
});

// ...existing code...
app.post("/register", (req, res) => {
    // 1. Input validation (whitelist)
    const username = req.body.username;
    const password = req.body.password;
    const usernameRegex = /^[A-Za-z0-9_]{3,30}$/;
    if (!username || !usernameRegex.test(username)) {
        return res.status(400).send("Invalid username");
    }
    if (!password || password.length < 12) {
        return res.status(400).send("Password must be at least 12 characters");
    }

    // 2. Password hashing with scrypt and per-user salt
    const salt = crypto.randomBytes(16).toString("hex");
    crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error");
        }
        const passwordHash = derivedKey.toString("hex");

        // 3. Parameterized insert to prevent SQL injection
        pool.query(
            "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
            [username, passwordHash, salt],
            (dbErr) => {
                if (dbErr) {
                    if (dbErr.code === "ER_DUP_ENTRY") {
                        return res.status(409).send("Username already exists");
                    }
                    console.error("DB error:", dbErr);
                    return res.status(500).send("Internal server error");
                }
                res.status(201).send("User registered successfully");
            }
        );
    });
});
// ...existing code...