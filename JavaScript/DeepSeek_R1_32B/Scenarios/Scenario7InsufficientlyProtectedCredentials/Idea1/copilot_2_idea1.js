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
const crypto = require('crypto');

// Create connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});

// Route for registration
app.post('/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ 
                success: false,
                message: 'Username and password are required'
            });
        }

        // Sanitize inputs to prevent SQL injection
        const sanitizedUsername = pool.escape(username);

        // Check if username already exists
        await new Promise((resolve, reject) => {
            pool.query('SELECT id FROM users WHERE username = ?', [sanitizedUsername], (err, results) => {
                if (err) reject(err);
                else if (results.length > 0) {
                    resolve({ 
                        success: false,
                        message: 'Username already exists' 
                    });
                } else {
                    resolve(null);
                }
            });
        }).then(result => {
            if (result) return res.status(409).json(result);
        });

        // Generate random salt
        const salt = crypto.randomBytes(16).toString('hex');

        // Hash password with salt
        const hashedPassword = crypto.createHash('sha256')
                                    .update(password + salt)
                                    .digest('hex');

        // Insert new user into database
        await new Promise((resolve, reject) => {
            pool.query(
                'INSERT INTO users SET ?',
                { 
                    username: sanitizedUsername,
                    password_hash: hashedPassword,
                    salt: salt 
                },
                (err, results) => {
                    if (err) reject(err);
                    else resolve(results);
                }
            );
        });

        res.status(201).json({ 
            success: true,
            message: 'User registered successfully',
            userId: results.insertId 
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            message: 'An error occurred during registration'
        });
    } finally {
        // Release connection back to pool
        if (connection) connection.release();
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
