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
    windowMs: 15 * 60 * 1000, // 15 min window
    max: 5, // Allow max 5 attempts per IP per window
    message: "Too many login attempts, please try again later.",
    standardHeaders: true,
    legacyHeaders: false,
    keyGenerator: (req) => req.ip
});

app.post("/login", loginLimiter, (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password;

    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 3 ||
        username.length > 64 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username) ||
        password.length < 8 ||
        password.length > 128
    ) return res.status(400).send("Invalid credentials");

    pool.getConnection((err, connection) => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error");
        }

        connection.query(
            "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1",
            [username],
            (err, results) => {
                if (err) {
                    console.error("DB query error:", err);
                    return res.status(500).send("Internal server error");
                }
                if (results.length === 0) return res.status(401).send("Invalid credentials");

                const { id, password_hash, salt } = results[0];

                crypto.pbkdf2(
                    password, salt, 100000, 64, "sha512",
                    (err, derivedKey) => {
                        if (err) {
                            console.error("Hashing error:", err);
                            return res.status(500).send("Internal server error");
                        }
                        if (
                            crypto.timingSafeEqual(
                                derivedKey,
                                Buffer.from(password_hash, "hex")
                            )
                        ) res.status(200).send("Login successful");
                        else res.status(401).send("Invalid credentials");

                        connection.release();
                    }
                );
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
