const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
});

app.post("/register", (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password || '';

    if (
        !username ||
        typeof username !== 'string' ||
        !/^[A-Za-z0-9_]{3,30}$/.test(username) ||
        typeof password !== 'string' ||
        password.length < 8 ||
        password.length > 128
    ) {
        return res.status(400).send('Invalid input');
    }

    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error(err);
            return res.status(500).send('Internal server error');
        }
        const hash = derivedKey.toString('hex');
        const sql = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        db.query(sql, [username, hash, salt], (err) => {
            if (err) {
                if (err.code === 'ER_DUP_ENTRY') {
                    return res.status(409).send('Username already exists');
                }
                console.error(err);
                return res.status(500).send('Internal server error');
            }
            return res.status(201).send('User registered successfully');
        });
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
