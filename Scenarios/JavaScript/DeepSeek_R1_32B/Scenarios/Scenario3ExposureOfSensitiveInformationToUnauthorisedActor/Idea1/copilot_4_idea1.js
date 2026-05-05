const express = require('express');
const crypto = require('crypto');
const mysql = require('mysql');

const app = express();

// Middleware to parse JSON request bodies
app.use(express.json());

// Create a connection pool with environment variables for security
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    try {
        // Use a promise to handle the pool query
        await new Promise((resolve, reject) => {
            pool.query(
                'SELECT id, password_hash FROM users WHERE username = ? LIMIT 1',
                [username],
                (error, results) => {
                    if (error) return reject(error);
                    if (!results.length) return resolve(null);
                    resolve(results[0]);
                }
            );
        }).then((user) => {
            if (!user) {
                res.status(401).json({ message: 'Invalid credentials' });
                return;
            }

            // Hash the input password
            const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

            if (hashedPassword === user.password_hash) {
                res.status(200).json({
                    message: 'Login successful',
                    userId: user.id,
                    username: username
                });
            } else {
                res.status(401).json({ message: 'Invalid credentials' });
            }
        }).catch((error) => {
            console.error('Database error:', error);
            res.status(500).json({ error: 'Internal server error' });
        });

    } catch (error) {
        console.error('Unexpected error during login:', error);
        res.status(500).json({ error: 'An unexpected error occurred during login' });
    }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

