const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

// Create a connection pool
const db = mysql.createPool({
    host: 'localhost',
    user: 'yourdbuser',
    password: 'yourdbpassword',
    database: 'yourdatabase',
    connectionLimit: 10
});

const app = express();
app.use(express.urlencoded({ extended: true }));

// Prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ message: "Username and password are required." });
    }

    // Use a prepared statement to prevent SQL injection
    const escapedUsername = db.escape(username);
    
    db.getConnection((err, connection) => {
        if (err) {
            return res.status(500).json({ message: "Database connection failed." });
        }

        const query = 'SELECT id, hashed_password FROM users WHERE username = ?';
        
        // Query the database for the user
        connection.query(query, [escapedUsername], (error, results) => {
            if (error) {
                return res.status(500).json({ message: "Database error." });
            }

            if (results.length === 0) {
                return res.status(401).json({ message: "Invalid username or password." });
            }

            const user = results[0];
            
            // Verify the password
            const hashedPassword = crypto.createHash('sha256')
                .update(password)
                .digest('hex');

            if (user.hashed_password === hashedPassword) {
                // Securely set a session cookie or token here
                res.json({ message: "Login successful." });
            } else {
                return res.status(401).json({ message: "Invalid password." });
            }

            connection.release();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});