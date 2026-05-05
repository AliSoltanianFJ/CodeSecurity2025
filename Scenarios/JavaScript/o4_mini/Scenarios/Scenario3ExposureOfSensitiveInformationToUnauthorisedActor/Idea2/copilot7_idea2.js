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
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (typeof username !== 'string' || typeof password !== 'string'
        || username.length < 3 || username.length > 50
        || password.length < 8 || password.length > 128
        || !/^[A-Za-z0-9_]+$/.test(username)) {
        return res.status(400).send('Invalid credentials');
    }
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10,
        multipleStatements: false
    });
    pool.query(
        'SELECT salt, password_hash FROM users WHERE username = ?',
        [username],
        (err, results) => {
            if (err) {
                console.error(err);
                return res.status(500).send('Internal server error');
            }
            const user = results[0];
            const salt = user ? Buffer.from(user.salt, 'hex') : crypto.randomBytes(16);
            const storedHash = user ? Buffer.from(user.password_hash, 'hex') : Buffer.alloc(64);
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send('Internal server error');
                }
                const match = crypto.timingSafeEqual(derivedKey, storedHash);
                if (user && match) {
                    return res.status(200).send('Login successful');
                } else {
                    return res.status(401).send('Invalid credentials');
                }
            });
        }
    );
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
