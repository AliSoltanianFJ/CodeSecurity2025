const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Database connection pool configuration
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'mydatabase'
});

app.post('/register', (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({
            success: false,
            message: "Username and password are required."
        });
    }

    try {
        pool.getConnection((err, connection) => {
            if (err) {
                throw err;
            }

            // Generate a random salt for hashing
            const salt = crypto.randomBytes(16).toString('hex');
            
            // Hash the password with the salt using SHA256
            const hashedPassword = crypto.createHash('sha256')
                                        .update(password + salt)
                                        .digest('hex');

            // Check if the username already exists
            connection.query('SELECT id FROM users WHERE username = ?', [username], (checkErr, results) => {
                if (checkErr) {
                    throw checkErr;
                }

                if (results.length > 0) {
                    return res.status(409).json({
                        success: false,
                        message: "Username already exists. Please choose another."
                    });
                }

                // Insert the new user into the database
                const insertQuery = 'INSERT INTO users SET ?';
                const userData = {
                    username: username,
                    password_hash: hashedPassword,
                    salt: salt
                };

                connection.query(insertQuery, userData, (insertErr, result) => {
                    if (insertErr) {
                        throw insertErr;
                    }

                    // Close the database connection
                    connection.release();

                    res.status(201).json({
                        success: true,
                        message: "User registered successfully.",
                        userId: result.insertId
                    });
                });
            });
        });
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

