const express = require('express');
const crypto = require('crypto');
const mysql = require('mysql2/promise');

require('dotenv').config();

// Create a pool using environment variables for security
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
});

const app = express();

app.use(express.json());

// Login route
app.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ message: 'Username and password are required' });
        }

        // Query the database for the user
        const [user] = await pool.query('SELECT id, username, password_hash FROM users WHERE username = ?', [username]);

        if (!user) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Hash the input password using SHA-256
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

        // Compare with stored hash
        if (hashedPassword !== user.password_hash) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Generate a session token or JWT here as needed
        const sessionId = crypto.randomBytes(64).toString('hex');

        res.status(200).json({
            message: 'Login successful',
            user_id: user.id,
            username: user.username,
            session_token: sessionId
        });
    } catch (error) {
        console.error('Error during login:', error);
        res.status(500).json({ message: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

