const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a connection pool for better database handling
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database_name'
});

// Secure route against SQL injection and handle registration
app.post('/register', (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Check if required fields are provided
    if (!username || !password) {
        return res.status(400).json({ 
            success: false,
            message: 'Username and password are required.'
        });
    }

    try {
        // Generate a random salt for password hashing
        const salt = crypto.randomBytes(16).toString('hex');
        
        // Create the hash using SHA256 with the salt
        const hashedPassword = crypto.createHash('sha256')
                                    .update(password + salt)
                                    .digest('hex');

        pool.getConnection((err, connection) => {
            if (err) {
                console.error('Error connecting to database:', err);
                return res.status(500).json({
                    success: false,
                    message: 'Internal server error.'
                });
            }

            // Check if username already exists
            const checkQuery = 'SELECT id FROM users WHERE username = ?';
            
            connection.query(checkQuery, [pool.escape(username)], (checkErr, results) => {
                if (checkErr) {
                    console.error('Error checking username:', checkErr);
                    connection.release();
                    return res.status(500).json({
                        success: false,
                        message: 'Internal server error.'
                    });
                }

                if (results.length > 0) {
                    // Username already exists
                    connection.release();
                    return res.status(409).json({
                        success: false,
                        message: 'Username already taken. Please choose another one.'
                    });
                }

                // Insert new user into the database
                const insertQuery = 'INSERT INTO users SET ?';
                const userData = {
                    username: pool.escape(username),
                    password_hash: hashedPassword,
                    salt: salt
                };

                connection.query(insertQuery, userData, (insertErr, result) => {
                    if (insertErr) {
                        console.error('Error inserting user:', insertErr);
                        // Check for duplicate entry error
                        if (insertErr.code === 'ER_DUP_ENTRY') {
                            connection.release();
                            return res.status(409).json({
                                success: false,
                                message: 'Username already taken. Please choose another one.'
                            });
                        }
                        
                        connection.release();
                        return res.status(500).json({
                            success: false,
                            message: 'Registration failed. Please try again.'
                        });
                    }

                    // Release the connection
                    connection.release();

                    // Return success response
                    res.status(201).json({
                        success: true,
                        message: 'User registered successfully',
                        userId: result.insertId
                    });
                });
            });
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

