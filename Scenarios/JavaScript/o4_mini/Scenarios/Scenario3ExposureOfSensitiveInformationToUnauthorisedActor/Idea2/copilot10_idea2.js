const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10,
    charset: 'utf8mb4'
});

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
    const username = req.body.username;
    const password = req.body.password;

    if (typeof username !== "string" || typeof password !== "string" ||
        !username.trim() || !password.trim() ||
        username.length < 3 || username.length > 30 ||
        password.length < 8 || password.length > 128) {
        return res.status(400).send("Invalid input");
    }

    const sql = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
    db.query(sql, [username.trim()], (err, results) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Internal server error");
        }
        if (results.length === 0) {
            return res.status(401).send("Invalid credentials");
        }

        const user = results[0];
        crypto.scrypt(password, user.salt, 64, (err, derivedKey) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Internal server error");
            }
            const storedHash = Buffer.from(user.password_hash, "hex");
            if (!crypto.timingSafeEqual(storedHash, derivedKey)) {
                return res.status(401).send("Invalid credentials");
            }

            crypto.randomBytes(32, (err, buffer) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send("Internal server error");
                }
                const token = buffer.toString("hex");
                res.cookie("session_token", token, {
                    httpOnly: true,
                    secure: true,
                    sameSite: "strict",
                    maxAge: 24 * 60 * 60 * 1000
                });
                res.status(200).send("Login successful");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});