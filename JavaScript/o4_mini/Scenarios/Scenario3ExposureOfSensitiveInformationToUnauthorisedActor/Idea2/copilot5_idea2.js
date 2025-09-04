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

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
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

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input' });
    }
    const sanitizedUsername = username.trim();
    if (sanitizedUsername.length === 0 || sanitizedUsername.length > 50 || password.length > 128) {
        return res.status(400).json({ error: 'Invalid input' });
    }

    db.query(
        'SELECT id, password_hash, salt FROM users WHERE username = ?',
        [sanitizedUsername],
        (err, results) => {
            if (err) {
                console.error('Database error');
                return res.sendStatus(500);
            }
            if (results.length === 0) {
                return res.status(401).json({ error: 'Authentication failed' });
            }
            const { id, password_hash, salt } = results[0];
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error('Hashing error');
                    return res.sendStatus(500);
                }
                const hashBuffer = Buffer.from(password_hash, 'hex');
                if (!crypto.timingSafeEqual(derivedKey, hashBuffer)) {
                    return res.status(401).json({ error: 'Authentication failed' });
                }
                res.status(200).json({ message: 'Login successful', userId: id });
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
