const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs");

// Set up database connection pool with secure options
const db = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database',
  insecureAuth: true // This is generally not recommended for production; use proper SSL configuration instead.
});

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting to prevent brute force attacks
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use('/login', limiter);

/**
 * Log in the user if the username and password are correct.
 */
app.post("/login", async (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ message: "Please provide both username and password" });
    }

    try {
        // Sanitize the username to prevent SQL injection
        const sanitizedUsername = mysql.escape(username);

        // Check if user exists
        const [user] = await db.promise().query(
            'SELECT id, password_hash FROM users WHERE username = ?',
            [sanitizedUsername]
        );

        if (!user || !user.id) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        // Validate password
        const isValidPassword = await bcrypt.compare(password, user.password_hash);
        
        if (isValidPassword) {
            // Generate a JSON Web Token or create a session for authentication
            const token = generateToken(user.id);

            res.status(200).json({ message: "Login successful", token });
        } else {
            return res.status(401).json({ message: "Invalid credentials" });
        }
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ message: "Internal server error" });
    }
});

function generateToken(userId) {
    // Implement token generation logic here, e.g., using jsonwebtoken
    const token = crypto.randomUUID(); // Example using UUIDv4; in practice, use a library like jsonwebtoken
    return token;
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});