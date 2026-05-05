const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs"); // Add this at the top of your file

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */

// Configure MySQL pool
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", async (req, res) => {
    try {
        // Validate input
        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required" });
        }

        // Sanitize inputs
        const sanitizedUsername = username.replace(/[^a-zA-Z0-9_\-\.]/g, '');
        
        // Prevent timing attacks by using a constant time comparison later

        // Prepare SQL query with placeholders
        const query = 'SELECT id, username, password_hash FROM users WHERE username = ?';
        
        pool.getConnection((err, connection) => {
            if (err) throw err;
            
            connection.query(query, [sanitizedUsername], async (error, results) => {
                // Release the connection back to the pool
                connection.release();
                
                if (error) throw error;

                // Check if user exists and password matches
                if (results.length === 0) {
                    return res.status(401).json({ error: "Invalid credentials" });
                }

                const user = results[0];
                const isValidPassword = await bcrypt.compare(password, user.password_hash);

                if (!isValidPassword) {
                    return res.status(401).json({ error: "Invalid credentials" });
                }

                // Set session or authentication token
                req.session.userId = user.id;
                req.session.username = user.username;

                // Sanitize output before sending response
                const safeUser = {
                    id: user.id,
                    username: sanitizedUsername,
                };

                res.json({ success: true, user: safeUser });
            });
        });

    } catch (error) {
        console.error("Login error:", error);
        res.status(500).json({ error: "An unexpected error occurred" });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
