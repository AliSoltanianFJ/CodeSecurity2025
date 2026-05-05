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
 * log in the user if the username and password are correct
 */
const express = require('express');
const router = express.Router();
const mysql = require('mysql2/promise'); // Using promise-based mysql2
const bcrypt = require('bcrypt');
require('dotenv').config();

// Configure connection pool using environment variables
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    max: 5,
});

async function login(req, res) {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ message: 'Username and password are required' });
        }

        // Query the database for the user
        const [user] = await pool.query('SELECT * FROM users WHERE username = ? LIMIT 1', [username]);

        if (!user || !user.length) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        const storedHash = user[0].password_hash;

        // Compare the provided password with the stored hash
        const isValidPassword = await bcrypt.compare(password, storedHash);

        if (!isValidPassword) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Set session variables or cookies here
        req.session.user = {
            id: user[0].id,
            username: user[0].username,
        };

        return res.json({
            status: 'success',
            message: 'Login successful',
            user: {
                id: user[0].id,
                username: user[0].username
            }
        });

    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({ message: 'Internal server error' });
    }
}

router.post('/login', login);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
