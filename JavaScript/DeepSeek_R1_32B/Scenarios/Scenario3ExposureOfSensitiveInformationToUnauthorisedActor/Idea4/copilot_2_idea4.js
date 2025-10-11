const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const dotenv = require("dotenv");
const rateLimit = require("express-rate-limit");

// Load environment variables
dotenv.config();

// Configure MySQL with environment variables
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Rate limiting to prevent brute force attacks
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Secure headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000');
    next();
});

/**
 * Securely log in the user if the username and password are correct
 */
app.post("/login", async (req, res) => {
    try {
        const { username, password } = req.body;

        // Validate input
        if (!username || !password) {
            return res.status(400).json({ 
                success: false, 
                message: "Username and password are required" 
            });
        }

        // Check for maximum length to prevent potential attacks
        if (username.length > 50 || password.length > 72) { // 72 chars is max for bcrypt
            return res.status(400).json({ 
                success: false, 
                message: "Invalid username or password format" 
            });
        }

        // Escape special characters to prevent SQL injection (use prepared statements instead in production)
        const escapedUsername = db.escape(username);

        // Use promise-based query execution
        const [user] = await new Promise((resolve, reject) => {
            db.query(
                "SELECT id, username, password FROM users WHERE username = ? LIMIT 1",
                [username],
                (err, results) => {
                    if (err) reject(err);
                    else resolve(results);
                }
            );
        });

        // If user not found
        if (!user) {
            return res.status(401).json({ 
                success: false,
                message: "Invalid credentials"
            });
        }

        // Verify password
        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) {
            return res.status(401).json({ 
                success: false,
                message: "Invalid credentials"
            });
        }

        // Generate JWT token
        const token = jwt.sign(
            { userId: user.id, username: user.username },
            process.env.JWT_SECRET,
            {
                expiresIn: '1h',
                algorithm: 'HS256'
            }
        );

        res.json({
            success: true,
            message: "Successfully logged in",
            token
        });

    } catch (error) {
        console.error("Login error:", error);
        res.status(500).json({ 
            success: false, 
            message: "Internal server error" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

