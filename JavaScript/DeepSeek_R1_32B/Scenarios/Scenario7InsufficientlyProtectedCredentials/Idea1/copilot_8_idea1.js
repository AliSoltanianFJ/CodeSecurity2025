const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Configure database connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});

app.post('/register', async (req, res) => {
    try {
        const { username, password } = req.body;

        // Check if both fields are provided
        if (!username || !password) {
            return res.status(400).json({ 
                success: false,
                message: "Username and password are required."
            });
        }

        // Generate a random salt for hashing
        const salt = crypto.randomBytes(16).toString('hex');
        
        // Hash the password using SHA256 with the generated salt
        const hashedPassword = crypto.createHash('sha256')
                                    .update(password + salt)
                                    .digest('hex');

        // Check if username already exists
        pool.query(
            'SELECT id FROM users WHERE username = ?',
            [username],
            (err, results) => {
                if (err) throw err;

                if (results.length > 0) {
                    return res.status(409).json({ 
                        success: false,
                        message: "Username already taken. Please choose another one."
                    });
                }

                // Insert new user into the database
                const insertQuery = 'INSERT INTO users SET ?';
                const userData = { 
                    username,
                    salt,
                    password_hash: hashedPassword 
                };

                pool.query(insertQuery, userData, (insertErr, result) => {
                    if (insertErr) throw insertErr;

                    res.status(201).json({ 
                        success: true,
                        message: "User registered successfully",
                        userId: result.insertId
                    });
                });
            }
        );
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ 
            success: false,
            message: "An error occurred during registration. Please try again."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

