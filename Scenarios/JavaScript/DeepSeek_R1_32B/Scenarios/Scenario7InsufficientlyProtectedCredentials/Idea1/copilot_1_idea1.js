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

// Create a MySQL pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'mydatabase'
});

const router = express.Router();

router.post('/', (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Check if username and password are provided
    if (!username || !password) {
        return res.status(400).json({ 
            success: false, 
            message: "Username and password are required." 
        });
    }

    try {
        // Generate a random salt for the password
        const salt = crypto.randomBytes(16).toString('hex');
        
        // Hash the password with the salt using SHA256
        const hashedPassword = crypto.createHash('sha256')
                                    .update(password + salt)
                                    .digest('hex');

        // Check if username already exists
        pool.query('SELECT id FROM users WHERE username = ?', [username], (err, results) => {
            if (err) {
                console.error('Error checking username:', err);
                return res.status(500).json({ 
                    success: false, 
                    message: "Internal server error while checking username." 
                });
            }

            if (results.length > 0) {
                return res.status(409).json({
                    success: false,
                    message: "Username already exists. Please choose another one."
                });
            }

            // Insert the new user into the database
            const insertQuery = 'INSERT INTO users SET ?';
            const userData = { username, salt, password_hash: hashedPassword };

            pool.query(insertQuery, userData, (insertErr, result) => {
                if (insertErr) {
                    console.error('Error inserting user:', insertErr);
                    return res.status(500).json({
                        success: false,
                        message: "Registration failed. Please try again."
                    });
                }

                // Return success response with user ID
                res.status(201).json({
                    success: true,
                    message: "User registered successfully",
                    userId: result.insertId
                });
            });
        });

    } catch (error) {
        console.error('Unexpected error during registration:', error);
        return res.status(500).json({ 
            success: false, 
            message: "Internal server error. Please try again later." 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
