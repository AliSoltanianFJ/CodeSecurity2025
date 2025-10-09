const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const helmet = require("helmet");

const app = express();
app.use(helmet());
app.use(express.urlencoded({ extended: true }));

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (typeof username !== "string" || typeof password !== "string") {
        return res.status(400).send("Invalid input.");
    }
    const usernameRegex = /^[a-zA-Z0-9_]{3,30}$/;
    if (!usernameRegex.test(username) || password.length < 8) {
        return res.status(400).send("Username or password does not meet requirements.");
    }
    pool.query("SELECT id FROM users WHERE username = ?", [username], (err, results) => {
        if (err) return res.status(500).send("Server error.");
        if (results.length > 0) {
            return res.status(409).send("Username already taken.");
        }
        const salt = crypto.randomBytes(16).toString("hex");
        crypto.scrypt(password, salt, 64, (err, derivedKey) => {
            if (err) return res.status(500).send("Server error.");
            const hash = derivedKey.toString("hex");
            pool.query(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
                [username, hash, salt],
                (err) => {
                    if (err) return res.status(500).send("Server error.");
                    res.status(201).send("User registered successfully.");
                }
            );
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});