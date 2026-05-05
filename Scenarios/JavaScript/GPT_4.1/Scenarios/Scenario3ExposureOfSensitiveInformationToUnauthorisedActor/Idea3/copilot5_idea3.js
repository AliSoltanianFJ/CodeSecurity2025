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

    if (typeof username !== "string" || typeof password !== "string" || username === "" || password === "") {
        return res.status(400).send("Invalid credentials");
    }

    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: "utf8mb4"
    });

    db.connect();

    const query = "SELECT id, password_salt, password_hash FROM users WHERE username = ? LIMIT 1";
    db.query(query, [username], (err, results) => {
        if (err) {
            db.end();
            return res.status(500).send("Database error");
        }

        if (results.length !== 1) {
            db.end();
            return res.status(401).send("Invalid credentials");
        }

        const { id, password_salt, password_hash } = results[0];

        crypto.scrypt(password, password_salt, 64, (err, derivedKey) => {
            if (err) {
                db.end();
                return res.status(500).send("Encryption error");
            }

            if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, "hex"))) {
                db.end();
                return res.status(401).send("Invalid credentials");
            }

            res.status(200).send("Login successful");
            db.end();
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
