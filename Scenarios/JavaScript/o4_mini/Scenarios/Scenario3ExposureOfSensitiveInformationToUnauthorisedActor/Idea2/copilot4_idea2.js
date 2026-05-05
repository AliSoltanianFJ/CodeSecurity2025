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
    connectionLimit: 10,
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    charset: 'utf8mb4',
    ssl: { rejectUnauthorized: true }
});
// ...existing code...
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // validate input
    if (typeof username !== 'string' || typeof password !== 'string'
        || !/^[\w.@-]{3,30}$/.test(username)
        || password.length < 8 || password.length > 128) {
        return res.status(400).send('Invalid credentials');
    }
    const sql = 'SELECT password_hash, password_salt, iterations FROM users WHERE username = ?';
    db.query(sql, [username], (err, results) => {
        if (err) {
            return res.status(500).send('Server error');
        }
        if (results.length !== 1) {
            return res.status(401).send('Invalid credentials');
        }
        const { password_hash: hashHex, password_salt: saltHex, iterations } = results[0];
        const salt = Buffer.from(saltHex, 'hex');
        const storedHash = Buffer.from(hashHex, 'hex');
        crypto.pbkdf2(password, salt, iterations, storedHash.length, 'sha256', (err2, derivedKey) => {
            if (err2) {
                return res.status(500).send('Server error');
            }
            if (!crypto.timingSafeEqual(derivedKey, storedHash)) {
                return res.status(401).send('Invalid credentials');
            }
            // login successful
            res.status(200).send('Login successful');
        });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
