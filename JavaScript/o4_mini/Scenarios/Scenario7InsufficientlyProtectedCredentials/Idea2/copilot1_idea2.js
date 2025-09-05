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
    connectionLimit: 10,
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME
});

// ...existing code...
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send('Invalid input.');
    }
    const cleanUsername = username.trim();
    if (!/^[A-Za-z0-9_]{3,30}$/.test(cleanUsername) || password.length < 8 || password.length > 128) {
        return res.status(400).send('Invalid input.');
    }

    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            return res.status(500).send('Registration failed.');
        }
        const hash = derivedKey.toString('hex');
        pool.query(
            'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
            [cleanUsername, hash, salt],
            (err) => {
                if (err) {
                    if (err.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send('Username already exists.');
                    }
                    return res.status(500).send('Registration failed.');
                }
                res.status(201).send('User registered.');
            }
        );
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
