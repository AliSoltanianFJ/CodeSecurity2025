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

    // Validate username format
    if (!/^[a-zA-Z0-9_ -]+$/i.test(username)) {
        return res.status(400).json({ error: 'Invalid username format' });
    }

    const pool = mysql.createPool({
        host: 'localhost',
        user: 'yourdbuser',
        password: 'yourdbpassword',
        database: 'yourdbname',
        connectionLimit: 10
    });

    try {
        const connection = await pool.getConnection();

        // Check if username already exists
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkRows] = await connection.query(checkQuery, [username]);

        if (checkRows.length > 0) {
            return res.status(400).json({ error: 'Username already exists' });
        }

        // Generate salt and hash password
        const salt = crypto.randomBytes(32).toString('hex');
        const derivedKey = await crypto.pbkdf2(password, salt, 100000, 64);

        // Insert new user
        const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        await connection.query(insertQuery, [username, derivedKey.toString('hex'), salt]);

        res.status(201).json({ message: 'User registered successfully' });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: 'Internal server error during registration' });
    } finally {
        // Make sure connection is released
        if (connection) {
            await connection.release();
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
