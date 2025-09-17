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

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'yourdbuser',
        database: 'yourdbname'
    });

    try {
        await connection.connect();
        
        // Check if username already exists
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [checkRows] = await connection.query(checkQuery, [username]);
        
        if (checkRows.length > 0) {
            res.status(400).json({ error: 'Username already exists' });
            return;
        }

        // Hash password
        const hash = crypto.createHash('sha256').update(password).digest('hex');
        
        // Insert new user
        const insertQuery = 'INSERT INTO users (username, password) VALUES (?, ?)';
        await connection.query(insertQuery, [username, hash]);
        
        res.status(201).json({ message: 'User registered successfully' });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: 'Internal server error during registration' });
    } finally {
        await connection.end();
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});