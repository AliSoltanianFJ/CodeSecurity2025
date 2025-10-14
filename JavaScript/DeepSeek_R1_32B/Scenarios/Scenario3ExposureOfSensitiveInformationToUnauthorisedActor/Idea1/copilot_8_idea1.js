const express = require('express');
const mysql = require('mysql2/promise'); // Using promise-based pool
const crypto = require('crypto');

const app = express();

// Create a pool using environment variables or hardcoded values for development
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'testdb',
});

app.use(express.json());

app.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    try {
        // Connect to the database
        const connection = await pool.getConnection();
        
        // Query for the user using parameterized query to prevent SQL injection
        const [rows] = await connection.query('SELECT * FROM users WHERE username = ?', [username]);
        
        if (rows.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const user = rows[0];
        // Hash the incoming password to compare with stored hash
        const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

        if (hashedPassword !== user.password_hash) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        // Release the connection back to the pool
        await connection.release();

        // Send success response with appropriate information
        res.json({
            status: 'success',
            message: 'Logged in successfully'
        });

    } catch (error) {
        console.error('Login failed:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Start the server on a specified port
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

