const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcrypt");
const xss = require("xss");

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    charset: 'utf8mb4',
    multipleStatements: false
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (!username || typeof username !== 'string' || username.trim().length < 3 || username.trim().length > 30) {
        return res.status(400).send('Invalid username');
    }
    if (!password || typeof password !== 'string' || password.length < 8 || password.length > 128) {
        return res.status(400).send('Invalid password');
    }
    const safeUsername = xss(username.trim());
    bcrypt.hash(password, 12, (err, hash) => {
        if (err) {
            console.error(err);
            return res.status(500).send('Internal Server Error');
        }
        db.query(
            'INSERT INTO users (username, password_hash) VALUES (?, ?)',
            [safeUsername, hash],
            (err) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send('Internal Server Error');
                }
                res.status(201).send('User registered');
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
