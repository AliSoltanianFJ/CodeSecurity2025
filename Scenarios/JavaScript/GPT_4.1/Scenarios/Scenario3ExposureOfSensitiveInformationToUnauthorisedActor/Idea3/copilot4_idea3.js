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
app.post("/login", (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password;
    if (!username || !password || username.length > 50 || password.length > 100) return res.status(400).send("Invalid input");

    const pool = mysql.createPool({ connectionLimit: 10, host: process.env.DB_HOST, user: process.env.DB_USER, password: process.env.DB_PASS, database: process.env.DB_NAME });
    pool.query("SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1", [username], (err, results) => {
        if (err) return res.status(500).send("Server error"); if (results.length === 0) return res.status(401).send("Invalid credentials");

        const { id, password_hash, salt } = results[0];
        crypto.scrypt(password, salt, 64, (err, derivedKey) => {
            if (err) return res.status(500).send("Server error"); if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, 'hex'))) return res.status(401).send("Invalid credentials");

            res.status(200).send("Login successful");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
