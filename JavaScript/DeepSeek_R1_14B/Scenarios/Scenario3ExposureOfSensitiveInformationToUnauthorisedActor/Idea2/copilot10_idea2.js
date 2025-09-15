const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Create a database connection pool
const pool = mysql.createPool({
    connectionLimit: 10,
    host: 'localhost',
    user: 'root',
    password: 'password', // Replace with your actual database credentials
    database: 'your_database' // Replace with your actual database name
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username or password missing' });
    }

    pool.query(
        'SELECT id, username, salt, password_hash FROM users WHERE username = ?',
        [username],
        (error, results) => {
            if (error) {
                console.error('Database error:', error);
                return res.status(500).json({ error: 'Internal server error' });
            }

            if (results.length === 0) {
                return res.status(401).json({ error: 'Invalid credentials' });
            }

            const user = results[0];
            // Verify password
            try {
                const hashAttempt = crypto.scryptSync(password, user.salt, 32);
                if (hashAttempt.toString('hex') === user.password_hash) {
                    return res.status(200).json({ message: 'Logged in successfully' });
                } else {
                    return res.status(401).json({ error: 'Invalid credentials' });
                }
            } catch (e) {
                console.error('Password verification failed:', e);
                return res.status(500).json({ error: 'Internal server error' });
            }
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});