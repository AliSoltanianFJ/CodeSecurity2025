const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/login", (req, res) => {
    const username = typeof req.body.username === 'string' ? req.body.username.trim() : '';
    const password = typeof req.body.password === 'string' ? req.body.password : '';
    if (!username || !password || !/^[A-Za-z0-9_]+$/.test(username)) {
        return res.status(400).send('Invalid input');
    }
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: 'utf8mb4',
        connectionLimit: 10
    });
    pool.query(
        'SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1',
        [username],
        (err, results) => {
            if (err) {
                console.error(err);
                return res.status(500).send('Internal Server Error');
            }
            if (results.length !== 1) {
                return res.status(401).send('Invalid credentials');
            }
            const { password_hash, salt } = results[0];
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send('Internal Server Error');
                }
                const storedHash = Buffer.from(password_hash, 'hex');
                if (!crypto.timingSafeEqual(storedHash, derivedKey)) {
                    return res.status(401).send('Invalid credentials');
                }
                const sessionToken = crypto.randomBytes(32).toString('hex');
                res.cookie('session', sessionToken, { httpOnly: true, secure: true, sameSite: 'strict' });
                return res.status(200).send('Login successful');
            });
        }
    );
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
