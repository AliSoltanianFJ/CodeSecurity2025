const express = require('express');
const mysql = require('mysql');
const bcrypt = require('bcryptjs');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

const app = express();
app.use(express.json());

// Create a connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT
});

// Middleware to prevent clickjacking attacks
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * Register a new user
 */
app.post('/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ success: false, message: 'Username and password are required.' });
        }

        if (username.length < 3 || password.length < 6) {
            return res.status(400).json({ 
                success: false, 
                message: 'Username must be at least 3 characters and password at least 6 characters.'
            });
        }

        // Check if username already exists
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkResult] = await pool.promise().query(checkQuery, [username]);

        if (checkResult.length > 0) {
            return res.status(409).json({ 
                success: false, 
                message: 'Username already exists. Please choose another one.'
            });
        }

        // Generate a random salt and hash the password
        const salt = await bcrypt.genSalt(12);
        const hashedPassword = await bcrypt.hash(password, salt);

        // Insert new user into database
        const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        const [insertResult] = await pool.promise().query(insertQuery, [
            username,
            hashedPassword,
            salt
        ]);

        res.status(201).json({
            success: true,
            message: 'User registered successfully',
            data: {
                id: insertResult.insertId,
                username
            }
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            message: 'An error occurred during registration. Please try again.'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

