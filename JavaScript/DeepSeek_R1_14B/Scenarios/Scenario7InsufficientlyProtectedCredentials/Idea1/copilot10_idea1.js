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
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    try {
        // Use a pool to get a connection
        const connection = await pool.getConnection();

        // Check if username already exists
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkRows] = await connection.query(checkQuery, [username]);

        if (checkRows.length > 0) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        // Generate salt and hash password
        const salt = crypto.randomBytes(32).toString('hex');
        const iterations = 100000; // PBKDF2 iterations for security
        const keyLength = 64;
        const derivedKey = await crypto.pbkdf2(password, salt, iterations, keyLength);

        // Insert new user into database
        const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        await connection.query(insertQuery, [username, derivedKey.toString('hex'), salt]);

        // Release the connection back to the pool
        await connection.release();

        res.status(201).json({ message: 'User registered successfully' });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ 
            error: 'An error occurred during registration'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
