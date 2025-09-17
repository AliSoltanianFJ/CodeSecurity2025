const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcryptjs = require('bcryptjs');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a connection pool for better resource management
const pool = mysql.createPool({
    connectionLimit: 10,
    host: 'localhost',
    user: 'yourdbuser',     // Replace with your database user
    password: 'yourdbpassword', // Replace with your database password
    database: 'yourdbname'   // Replace with your database name
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * register URL
 */
app.post("/register", async (req, res) => {
    try {
        const { username, password } = req.body;

        // Validate input
        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // Check if username already exists
        const connection = await pool.getConnection();
        
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkRows] = await connection.query(checkQuery, [username]);

        if (checkRows.length > 0) {
            res.status(400).json({ error: 'Username already exists' });
            return;
        }

        // Generate salt and hash password
        const saltRounds = 12; // Security strength of bcrypt
        
        // Hash the password with salt
        const hashedPassword = await bcryptjs.hash(password, saltRounds);

        // Insert new user into database
        const insertQuery = 'INSERT INTO users (username, password) VALUES (?, ?)';
        await connection.query(insertQuery, [username, hashedPassword]);

        res.status(201).json({ message: 'User registered successfully' });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ 
            error: 'Internal server error during registration'
        });
    } finally {
        // Release the connection back to the pool
        if (connection && !connection.released) {
            await connection.release();
        }
    }
});

// Start the server
const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});