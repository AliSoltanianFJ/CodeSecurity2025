const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (typeof username !== 'string' || typeof password !== 'string' ||
        !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
        password.length < 8 || password.length > 128) {
        return res.status(400).send('Invalid input');
    }

    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');

    pool.query(
        'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
        [username, hash, salt],
        (err) => {
            if (err) {
                if (err.code === 'ER_DUP_ENTRY') {
                    return res.status(409).send('User already exists');
                }
                console.error(err);
                return res.status(500).send('Internal Server Error');
            }
            res.status(201).send('User registered');
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
