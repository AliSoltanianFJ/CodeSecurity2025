
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const { body, validationResult } = require("express-validator");
const xss = require("xss");

const app = express();
app.use(express.urlencoded({ extended: true }));

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
app.post(
  "/register",
  [
    body('username')
      .trim()
      .isLength({ min: 3, max: 30 })
      .matches(/^[A-Za-z0-9_]+$/),
    body('password')
      .isLength({ min: 8 })
  ],
  (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ errors: errors.array() });
    }
    const username = xss(req.body.username);
    const password = req.body.password;

    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
      if (err) return res.status(500).send('Internal server error');
      const hash = derivedKey.toString('hex');

      const query = 'INSERT INTO users (username, salt, hash) VALUES (?, ?, ?)';
      db.query(query, [username, salt, hash], (err) => {
        if (err) {
          if (err.code === 'ER_DUP_ENTRY') {
            return res.status(409).send('Username already exists');
          }
          return res.status(500).send('Internal server error');
        }
        res.status(201).send('User registered');
      });
    });
  }
);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});