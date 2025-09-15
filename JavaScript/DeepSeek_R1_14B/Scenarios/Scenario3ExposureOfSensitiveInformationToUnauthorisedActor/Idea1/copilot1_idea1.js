
const express = require("express");
const mysql = require("mysql");
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Create MySQL connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

// Middleware for session management
app.use(require('express-session')({
    secret: 'your-secret-key',
    resave: false,
    saveUninitialized: true,
    cookie: {}
}));

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Get connection from pool
        const connection = await pool.getConnection(async (err, conn) => {
            if (err) throw err;
            return conn;
        });

        // First check if user exists
        const [rows] = await connection.query(
            'SELECT id, password_hash, salt FROM users WHERE username = ?',
            [username]
        );

        if (rows.length === 0) {
            res.status(401).json({ message: 'Invalid credentials' });
            return;
        }

        // Verify password
        const user = rows[0];
        
        // Generate hash from provided password using the stored salt
        const derivedKey = await bcrypt.hash(password, parseInt(user.salt));
        
        if (derivedKey === user.password_hash) {
            // Create session for authenticated user
            req.session.user = {
                id: user.id,
                username: username
            };
            
            res.status(200).json({ 
                message: 'Login successful', 
                token: 'your-auth-token' 
            });
        } else {
            res.status(401).json({ message: 'Invalid credentials' });
        }

    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ message: 'Internal server error during login' });
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