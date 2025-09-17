const express = require("express");
const crypto = require("crypto");
const mysql = require("mysql");
const bcryptjs = require('bcryptjs');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create database connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'yourdbuser',
    password: 'yourdbpassword',
    database: 'yourdbname',
    connectionLimit: 10
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/register", async (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    try {
        let connection;
        try {
            connection = await pool.getConnection();
            
            // Check if username already exists
            const checkQuery = 'SELECT id FROM users WHERE username = ?';
            const [checkRows] = await connection.query(checkQuery, [username]);
            
            if (checkRows.length > 0) {
                return res.status(409).json({ error: 'Username already exists' });
            }

            // Generate salt and hash password
            const salt = await bcryptjs.genSalt(12);
            const hashedPassword = await bcryptjs.hash(password, salt);

            // Insert new user
            const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
            await connection.query(insertQuery, [username, hashedPassword, salt]);

        } finally {
            if (connection) {
                await connection.release();
            }
        }

        return res.status(201).json({ message: 'User registered successfully' });

    } catch (error) {
        console.error('Registration error:', error);
        return res.status(500).json({ 
            error: 'Internal server error during registration'
        });
    }
});

// Start the server
app.listen(3000, () => {
    console.log('Server running on port 3000');
});