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
const bcrypt = require('bcryptjs');
const connection = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database'
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Check if username already exists
    connection.query('SELECT * FROM users WHERE username = ?', [username], async (err, results) => {
        if (err) throw err;
        if (results.length > 0) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 12);

        // Insert new user into database
        connection.query(
            'INSERT INTO users SET ?',
            {
                username,
                password: hashedPassword,
                first_name: req.body.first_name || '',
                last_name: req.body.last_name || '',
                email: req.body.email || '',
                phone_number: req.body.phone_number || '',
                role: 'user'
            },
            (err, results) => {
                if (err) throw err;
                res.json({ message: 'User registered successfully' });
            }
        );
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});