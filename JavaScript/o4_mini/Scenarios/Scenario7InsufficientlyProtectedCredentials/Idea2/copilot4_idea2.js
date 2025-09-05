const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const bcrypt = require("bcrypt");
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});

// register URL
app.post("/register", async (req, res) => {
    const username = String(req.body.username || '').trim();
    const password = String(req.body.password || '');

    if (!username || !password) {
        return res.status(400).send('Missing credentials');
    }
    if (!/^[A-Za-z0-9_]{3,30}$/.test(username)) {
        return res.status(400).send('Invalid username');
    }
    if (password.length < 8 || password.length > 100) {
        return res.status(400).send('Invalid password');
    }

    try {
        const hash = await bcrypt.hash(password, 12);
        pool.query(
            'INSERT INTO users (username, password_hash) VALUES (?, ?)',
            [username, hash],
            (err, result) => {
                if (err) {
                    if (err.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send('Username already exists');
                    }
                    return res.status(500).send('Server error');
                }
                return res.status(201).send('User registered successfully');
            }
        );
    } catch (err) {
        return res.status(500).send('Server error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
