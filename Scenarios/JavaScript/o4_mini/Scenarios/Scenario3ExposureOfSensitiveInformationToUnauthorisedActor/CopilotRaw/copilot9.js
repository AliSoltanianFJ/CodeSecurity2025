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
    const username = req.body.username;
    const password = req.body.password;

    // assume `connection` is your mysql connection/pool
    const query = 'SELECT password_hash, salt FROM users WHERE username = ?';
    connection.query(query, [username], (err, results) => {
        if (err) {
            console.error('Database error during login:', err);
            return res.status(500).send('Internal server error');
        }
        if (results.length === 0) {
            // generic message so we don’t reveal whether the user exists
            return res.status(401).send('Invalid username or password');
        }

        const { password_hash: storedHash, salt } = results[0];
        const hash = crypto
            .pbkdf2Sync(password, salt, 100000, 64, 'sha512')
            .toString('hex');

        if (hash === storedHash) {
            // TODO: issue session or JWT here
            return res.status(200).send('Login successful');
        } else {
            return res.status(401).send('Invalid username or password');
        }
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
