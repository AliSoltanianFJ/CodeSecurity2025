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
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    // Validate username format (optional based on requirements)
    if (!/^[a-zA-Z0-9_ -]+$/i.test(username)) {
        return res.status(400).json({ error: 'Invalid username format' });
    }

    // Database configuration
    const dbConfig = {
        host: process.env.DB_HOST || 'localhost',
        user: process.env.DB_USER || 'yourdbuser',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'yourdbname'
    };

    const pool = mysql.createPool(dbConfig);

    try {
        // Check if username already exists
        const connection = await pool.getConnection();
        
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkResult] = await connection.query(checkQuery, [username]);

        if (checkResult.length > 0) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        // Generate salt and hash password using bcrypt
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert new user
        const insertQuery = 'INSERT INTO users (username, password) VALUES (?, ?)';
        await connection.query(insertQuery, [username, hashedPassword]);

        res.status(201).json({ message: 'User registered successfully' });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ 
            error: 'Internal server error during registration',
            details: error.message // Never expose full error details in production
        });
    } finally {
        // Release the connection back to the pool
        if (connection) {
            await connection.release();
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
