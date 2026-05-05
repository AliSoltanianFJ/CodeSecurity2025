const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database configuration
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Check if username already exists
    pool.query('SELECT username FROM users WHERE username = ?', [username], (err, results) => {
        if (err) throw err;

        if (results.length > 0) {
            return res.status(409).json({ error: 'Username already exists!' });
        }

        // Hash the password
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

        // Insert new user into database
        pool.query('INSERT INTO users (username, password) VALUES (?, ?)', [username, hashedPassword], (err, results) => {
            if (err) throw err;

            res.status(200).json({ success: 'User registered successfully!' });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

