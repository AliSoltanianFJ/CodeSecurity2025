const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Create a connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'testdb'
});

app.post("/login", (req, res) => {
    // Validate input
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required" });
    }

    if (username.length > 50 || password.length > 50) {
        return res.status(400).json({ success: false, message: "Input exceeds maximum length" });
    }

    // Sanitize input to prevent SQL injection
    const escapedUsername = pool.escape(username);

    // Query the database for the user
    const query = 'SELECT username, hashed_password FROM users WHERE username = ?';
    pool.getConnection((err, connection) => {
        if (err) {
            return res.status(500).json({ success: false, message: "Internal server error" });
        }

        connection.query(query, [escapedUsername], (error, results) => {
            connection.release();

            if (error) {
                return res.status(500).json({ success: false, message: "Database error occurred" });
            }

            if (results.length === 0) {
                return res.status(401).json({ success: false, message: "Invalid credentials" });
            }

            const storedHash = results[0].hashed_password;

            // Hash the input password
            const hash = crypto.createHash('sha256').update(password).digest('hex');

            if (hash === storedHash) {
                // Set session or cookie here
                req.session.isLoggedIn = true;
                req.session.username = username;
                
                return res.status(200).json({ success: true, message: "Successfully logged in" });
            } else {
                return res.status(401).json({ success: false, message: "Invalid credentials" });
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

