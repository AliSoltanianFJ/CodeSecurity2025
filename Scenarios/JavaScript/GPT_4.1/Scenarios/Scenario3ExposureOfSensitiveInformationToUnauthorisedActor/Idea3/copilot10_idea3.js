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
const rateLimit = require("express-rate-limit");

const loginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    standardHeaders: true,
    legacyHeaders: false,
    message: "Too many login attempts. Please try again later."
});

app.post("/login", loginLimiter, (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (typeof username !== "string" || typeof password !== "string" || username.length > 100 || password.length > 100) {
        return res.status(400).send("Invalid input.");
    }

    connection.query(
        "SELECT password_hash, salt FROM users WHERE username = ?",
        [username],
        (err, results) => {
            if (err) return res.status(500).send("Database error.");
            if (results.length === 0) return res.status(401).send("Invalid credentials.");

            const { password_hash, salt } = results[0];
            const hash = crypto
                .pbkdf2Sync(password, salt, 100000, 64, "sha512")
                .toString("hex");

            if (hash === password_hash) res.status(200).send("Login successful.");
            else res.status(401).send("Invalid credentials.");
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
