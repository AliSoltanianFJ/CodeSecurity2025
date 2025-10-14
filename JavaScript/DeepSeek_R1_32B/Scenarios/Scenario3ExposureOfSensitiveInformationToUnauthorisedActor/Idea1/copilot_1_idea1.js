const express = require('express');
const crypto = require('crypto');
const mysql = require('mysql');
const ratelimit = require('express-rate-limit');

// Create Express app
const app = express();

// Set up rate limiter to prevent brute force attacks
const limiter = ratelimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
});

// Apply rate limiter to the login route
app.use('/api/login', limiter);

// Create a database pool using environment variables for security
const dbPool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
});

// Middleware to parse JSON request bodies
app.use(express.json());

// Secure headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

// Login route
app.post('/api/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required' });
        }

        // Escape special characters in the username to prevent SQL injection
        const escapedUsername = mysql.escape(username);

        // Query for the user by username
        const query = `SELECT id, password_hash FROM users WHERE username = ${escapedUsername} LIMIT 1`;
        
        const connection = await new Promise((resolve, reject) => {
            dbPool.getConnection((err, conn) => {
                if (err) return reject(err);
                resolve(conn);
            });
        });

        try {
            const [rows] = await new Promise((resolve, reject) => {
                connection.query(query, (err, results) => {
                    if (err) return reject(err);
                    resolve(results);
                });
            });

            if (!rows || rows.length === 0) {
                return res.status(401).json({ error: 'Invalid credentials' });
            }

            const storedHash = rows[0].password_hash;

            // Hash the input password
            const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

            if (hashedPassword !== storedHash) {
                return res.status(401).json({ error: 'Invalid credentials' });
            }

            // Generate a session token
            const sessionToken = crypto.randomBytes(64).toString('hex');
            
            // Insert the session into the database
            const sessionQuery = `INSERT INTO sessions (user_id, token) VALUES (${rows[0].id}, '${mysql.escape(sessionToken)}')`;
            
            await new Promise((resolve, reject) => {
                connection.query(sessionQuery, (err, results) => {
                    if (err) return reject(err);
                    resolve(results);
                });
            });

            // Close the database connection
            connection.release();

            res.status(200).json({
                message: 'Login successful',
                token: sessionToken,
                user_id: rows[0].id
            });

        } catch (error) {
            console.error('Login error:', error);
            res.status(500).json({ error: 'Internal server error' });
        } finally {
            if (connection && connection.state !== 'released') {
                connection.release();
            }
        }

    } catch (error) {
        console.error('Route error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

