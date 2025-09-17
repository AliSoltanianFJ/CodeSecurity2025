const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
const con = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database'
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    con.getConnection((err, connection) => {
        if (err) {
            return res.status(500).json({ error: 'Database connection failed!' });
        }

        // Check if username already exists
        connection.query('SELECT * FROM users WHERE username = ?', [username], (err, result) => {
            if (err) {
                return res.status(500).json({ error: 'Error checking username' });
            }

            if (result.length > 0) {
                return res.status(400).json({ error: 'Username already exists!' });
            }

            // Generate salt and hash password
            const salt = crypto.randomBytes(32);
            const derivedKey = crypto.pbkdf2Sync(password, salt, 1000, 64);

            // Insert new user
            connection.query(
                'INSERT INTO users (username, password_hash) VALUES (?, ?)',
                [username, derivedKey.toString('base64')],
                (err, result) => {
                    if (err) {
                        return res.status(500).json({ error: 'Error registering user' });
                    }
                    connection.release();
                    res.status(201).json({ message: 'User registered successfully!' });
                }
            );
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});