const express = require("express");
const mysql = require("mysql");
const crypto = require('crypto');
require('dotenv').config();

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
const port = process.env.PORT || 3000;

// Create MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        if (!username || !password) {
            return res.status(400).json({ message: 'Username and password required' });
        }

        // Generate hash from password
        const salt = crypto.randomBytes(16).toString('hex');
        const hash = crypto.createHash('sha256').update(password + salt).digest('hex');

        // Query for user existence with matching password hash
        await db.getConnection(async (err, connection) => {
            if (err) throw err;

            const [rows] = await connection.query(
                'SELECT id FROM users WHERE username = ? AND password_hash = ?',
                [username, hash]
            );

            connection.release();

            if (rows.length > 0) {
                // Generate a session token or JWT
                const token = crypto.randomBytes(16).toString('hex');
                
                res.status(200).json({
                    success: true,
                    message: "Login successful!",
                    token: token
                });
            } else {
                res.status(401).json({ 
                    success: false, 
                    message: "Invalid credentials" 
                });
            }
        });

    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ 
            error: 'Internal server error' 
        });
    }
});

app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});