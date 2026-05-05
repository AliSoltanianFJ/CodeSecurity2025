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
const pool = mysql.createPool({ /* secure config */ });

const loginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    message: "Too many login attempts, please try again later."
});

app.post("/login", loginLimiter, (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password;
    if (!username || !password) return res.sendStatus(400);

    const query = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
    pool.query(query, [username], (err, results) => {
        if (err) return res.sendStatus(500);
        if (results.length === 0) return res.sendStatus(401);

        const { id, password_hash, salt } = results[0];
        crypto.scrypt(password, salt, 64, (err, derivedKey) => {
            if (err) return res.sendStatus(500);
            if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, 'hex'))) return res.sendStatus(401);

            res.status(200).send("Login successful");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
