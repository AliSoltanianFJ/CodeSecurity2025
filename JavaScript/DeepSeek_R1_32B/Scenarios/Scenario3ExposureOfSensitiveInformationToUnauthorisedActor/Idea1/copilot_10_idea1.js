const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
const bcrypt = require("bcryptjs"); // Make sure to install bcryptjs

// Create a connection pool using environment variables for security
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
});

const express = require("express");
const router = express.Router();

router.post("/login", (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ message: "Please provide all required fields" });
    }

    // Escape the username to prevent SQL injection
    const escapedUsername = pool.escape(username);

    // Query for user with matching username
    pool.query(
        'SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1',
        [username],
        (error, results) => {
            if (error) {
                console.error("Database error:", error);
                return res.status(500).json({ message: "Internal server error" });
            }

            if (results.length === 0) {
                // No user found with that username
                return res.status(401).json({ message: "Invalid credentials" });
            }

            const user = results[0];
            
            // Compare the password hash using bcrypt
            bcrypt.compare(password, user.password_hash, (err, isMatch) => {
                if (err || !isMatch) {
                    return res.status(401).json({ message: "Invalid credentials" });
                }

                // Passwords match - create a session or token
                const sessionToken = crypto.randomBytes(64).toString("hex");

                res.json({
                    message: "Login successful",
                    user_id: user.id,
                    username: user.username,
                    token: sessionToken
                });
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
