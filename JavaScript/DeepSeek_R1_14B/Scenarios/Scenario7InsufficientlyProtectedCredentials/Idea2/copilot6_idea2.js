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
 * register URL
 */
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    if (username.length < 3 || username.length > 50) {
        return res.status(400).json({ error: "Username must be between 3 and 50 characters." });
    }

    if (password.length < 8 || password.length > 100) {
        return res.status(400).json({ error: "Password must be at least 8 characters." });
    }

    // Sanitize input to prevent SQL injection
    const sanitizedUsername = mysql.escape(username);

    // Validate against malicious characters for XSS prevention
    if (/['"]/g.test(username) || /<|>|\||\}|\\|`/g.test(password)) {
        return res.status(400).json({ error: "Invalid characters detected." });
    }

    // Password hashing using bcrypt
    const bcrypt = require('bcryptjs');
    
    try {
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Database connection
        const db = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: '',
            database: 'your_database'
        });

        await db.promise().query(
            `INSERT INTO users (username, password) VALUES (?, ?)`,
            [sanitizedUsername, hashedPassword]
        );

        res.status(201).json({ message: "User registered successfully." });
    } catch (err) {
        console.error("Registration error:", err);
        res.status(500).json({ error: "An error occurred during registration." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
