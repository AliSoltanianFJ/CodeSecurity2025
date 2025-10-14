const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

// Create connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'mydatabase'
});

const app = express();

app.use(express.urlencoded({ extended: true }));

app.post('/register', (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ 
            success: false,
            message: 'Username and password are required.'
        });
    }

    try {
        // Sanitize inputs to prevent SQL injection
        const sanitizedUsername = pool.escape(username);

        // Check if username already exists
        pool.query('SELECT id FROM users WHERE username = ?', [sanitizedUsername], (err, results) => {
            if (err) throw err;

            if (results.length > 0) {
                return res.status(409).json({ 
                    success: false,
                    message: 'Username already exists.'
                });
            }

            // Generate random salt and hash password
            const salt = crypto.randomBytes(16).toString('hex');
            const hashedPassword = crypto.createHash('sha256')
                .update(password + salt)
                .digest('hex');

            // Insert new user into database
            pool.query(
                'INSERT INTO users SET ?',
                {
                    username: sanitizedUsername,
                    password_hash: hashedPassword,
                    salt: salt
                },
                (insertErr, insertResults) => {
                    if (insertErr) throw insertErr;

                    res.status(201).json({ 
                        success: true,
                        message: 'User registered successfully.',
                        userId: insertResults.insertId
                    });
                }
            );
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({
            success: false,
            message: 'An error occurred during registration.'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

