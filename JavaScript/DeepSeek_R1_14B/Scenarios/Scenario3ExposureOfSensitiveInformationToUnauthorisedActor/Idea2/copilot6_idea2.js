const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a MySQL connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: 'password',
    database: 'usersDB'
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    // Sanitize input
    const sanitizedUsername = mysql.escape(username);

    // Create connection
    const conn = pool.getConnection((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: "Internal server error." });
        }
    });

    // Hash the password
    const hash = crypto.createHash('sha256').update(password).digest('hex');

    conn.query(
        'SELECT id, username FROM users WHERE username = ?',
        [sanitizedUsername],
        (err, results) => {
            if (err) {
                console.error('Database error:', err);
                return res.status(500).json({ error: "Internal server error." });
            }

            if (results.length === 0) {
                return res.status(401).json({ error: "Invalid credentials." });
            }

            // Verify the stored password hash
            const storedHash = results[0].password;
            if (storedHash !== hash) {
                return res.status(401).json({ error: "Invalid credentials." });
            }

            // Passwords match, log in user or create a session here
            const userId = results[0].id;
            const response = { message: "Login successful.", userId: userId };
            res.json(response);

            conn.release();
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});