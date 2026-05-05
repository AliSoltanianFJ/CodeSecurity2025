
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// secure headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// database pool
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const usernameRaw = req.body.username;
    const passwordRaw = req.body.password;
    if (typeof usernameRaw !== 'string' || typeof passwordRaw !== 'string') {
      return res.status(400).json({ message: 'Invalid input' });
    }
    const username = usernameRaw.trim();
    const password = passwordRaw;
    if (!/^[A-Za-z0-9_]{3,30}$/.test(username) || password.length < 8 || password.length > 128) {
      return res.status(400).json({ message: 'Invalid input' });
    }
    pool.execute(
      'SELECT password_hash, salt FROM users WHERE username = ?',
      [username],
      (err, results) => {
        if (err) {
          return res.status(500).json({ message: 'Server error' });
        }
        if (results.length === 0) {
          return res.status(401).json({ message: 'Invalid credentials' });
        }
        const { password_hash: storedHash, salt } = results[0];
        crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
          if (err) {
            return res.status(500).json({ message: 'Server error' });
          }
          const hashedPassword = derivedKey.toString('hex');
          if (!crypto.timingSafeEqual(Buffer.from(hashedPassword, 'hex'), Buffer.from(storedHash, 'hex'))) {
            return res.status(401).json({ message: 'Invalid credentials' });
          }
          return res.status(200).json({ message: 'Login successful' });
        });
      }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});