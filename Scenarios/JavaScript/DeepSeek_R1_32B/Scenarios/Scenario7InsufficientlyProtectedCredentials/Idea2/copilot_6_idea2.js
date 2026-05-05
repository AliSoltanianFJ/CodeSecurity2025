const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs");
const escape = require("escape-html");
const rateLimit = require("express-rate-limit");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Rate limiting to prevent brute force attacks
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 20 // limit each IP to 20 requests per windowMs
});
app.use('/register', limiter);

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Create a connection pool for better resource management
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    insecureAuth: false,
    multipleStatements: false, // Prevent SQL injection
});

/**
 * register URL with comprehensive security measures
 */
app.post("/register", async (req, res) => {
    try {
        const username = escape(String(req.body.username)).trim();
        const password = req.body.password;

        // Validate input length
        if (!username || !password) {
            return res.status(400).send("Username and password are required");
        }

        if (username.length > 50 || password.length < 8 || password.length > 72) {
            return res.status(400).send("Invalid username or password length");
        }

        // Generate a random salt using bcrypt
        const salt = await bcrypt.genSaltSync(12);
        
        // Hash the password with the generated salt
        const hashedPassword = await bcrypt.hash(password, salt);

        // Prepare SQL statement to prevent injection
        pool.query(
            "INSERT INTO users (username, password_hash) VALUES (?, ?)",
            [username, hashedPassword],
            (error, results) => {
                if (error) {
                    console.error("Registration error:", error);
                    return res.status(500).send("An error occurred during registration");
                }
                
                // Return success response
                res.status(201).json({ message: "User registered successfully" });
            }
        );
    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).send("Internal server error during registration");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

