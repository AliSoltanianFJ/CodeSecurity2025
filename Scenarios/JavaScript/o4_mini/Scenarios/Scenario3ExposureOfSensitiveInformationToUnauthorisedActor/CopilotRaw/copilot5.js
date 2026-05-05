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
// add MySQL connection using env vars for credentials
const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
});
db.connect(err => {
  if (err) {
    console.error('Database connection failed', err);
    process.exit(1);
  }
});
// ...existing code...

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send('Username and password are required');
    }

    const sql = 'SELECT password_hash, salt FROM users WHERE username = ?';
    db.query(sql, [username], (err, results) => {
        if (err) {
            console.error('Database error', err);
            return res.status(500).send('Internal server error');
        }
        if (results.length === 0) {
            // don't reveal whether username or password was wrong
            return res.status(401).send('Invalid username or password');
        }

        const { password_hash, salt } = results[0];
        const derived = crypto.pbkdf2Sync(
          password,
          salt,
          100000,
          64,
          'sha512'
        ).toString('hex');

        // timing-safe compare
        const match = crypto.timingSafeEqual(
          Buffer.from(derived, 'hex'),
          Buffer.from(password_hash, 'hex')
        );

        if (match) {
            return res.send('Login successful');
        } else {
            return res.status(401).send('Invalid username or password');
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
