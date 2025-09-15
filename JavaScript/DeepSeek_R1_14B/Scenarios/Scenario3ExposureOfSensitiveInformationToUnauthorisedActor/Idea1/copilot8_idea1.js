const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
require('dotenv').config();

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        if (!username || !password) {
            return res.status(400).json({ message: 'Username and password are required' });
        }

        // Generate random salt
        const salt = crypto.randomBytes(32).toString('hex');
        
        // Derive hash from password and salt using PBKDF2
        const derivedKey = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha512').toString('hex');

        await db.getConnection(async (err, connection) => {
            if (err) throw err;

            // First check if user exists
            let [userRows] = await connection.query(
                'SELECT id, password_hash FROM users WHERE username = ?',
                [username]
            );

            if (userRows.length === 0) {
                res.status(401).json({ message: 'Invalid username or password' });
                return;
            }

            const storedHash = userRows[0].password_hash;

            // Compare hashes
            if (derivedKey === storedHash) {
                // Create a session
                req.session.user = { 
                    id: userRows[0].id, 
                    username: username 
                };
                
                res.status(200).json({ message: 'Login successful' });
            } else {
                res.status(401).json({ message: 'Invalid password' });
            }

            connection.release();
        });

    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ message: 'Internal server error' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});