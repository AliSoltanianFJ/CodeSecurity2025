const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'mydatabase'
});

// Set security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

/**
 * Register a new user
 */
app.post('/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        // Validate input
        if (!username || !password) {
            return res.status(400).json({ 
                success: false,
                message: "Username and password are required."
            });
        }

        // Check if username already exists
        let connection;
        try {
            connection = await pool.promise().getConnection();
            const [existingUser] = await connection.query(
                'SELECT id FROM users WHERE username = ?',
                [username]
            );

            if (existingUser.length > 0) {
                return res.status(409).json({
                    success: false,
                    message: "Username already exists."
                });
            }

            // Generate a random salt for password hashing
            const salt = crypto.randomBytes(16).toString('hex');
            
            // Hash the password using SHA256 with the generated salt
            const hashedPassword = crypto.createHash('sha256')
                                        .update(password + salt)
                                        .digest('hex');

            // Insert new user into database
            const result = await connection.query(
                'INSERT INTO users SET ?',
                {
                    username: username,
                    password_hash: hashedPassword,
                    salt: salt
                }
            );

            res.status(201).json({
                success: true,
                message: "User registered successfully.",
                userId: result.insertId
            });

        } finally {
            if (connection) connection.release();
        }

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            message: "An error occurred during registration."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

