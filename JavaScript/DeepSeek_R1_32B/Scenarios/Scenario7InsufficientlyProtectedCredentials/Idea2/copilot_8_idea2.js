const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcryptjs = require("bcryptjs"); // Add this at the top

// Create connection pool (keep this outside route handler for better performance)
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/register", async (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password?.trim();

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Validate username format (only letters, numbers, underscores and hyphens)
    if (!/^[a-zA-Z0-9_-]{3,20}$/.test(username)) {
        return res.status(400).json({
            error: "Username must be 3-20 characters containing only letters, numbers, underscores or hyphens"
        });
    }

    // Validate password strength
    if (!/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+{}\[\]:;"'?/<>,.-]).{8,}$/.test(password)) {
        return res.status(400).json({
            error: "Password must be at least 8 characters with at least one uppercase letter, lowercase letter, number and special character"
        });
    }

    try {
        // Escape input to prevent SQL injection
        const escapedUsername = db.escape(username);
        
        // Hash password using bcryptjs
        const salt = await bcryptjs.genSalt(12); // 12 rounds of hashing
        const hashedPassword = await bcryptjs.hash(password, salt);

        // Check if username already exists
        const checkQuery = 'SELECT id FROM users WHERE username = ?';
        const [existingUser] = await db.promise().query(checkQuery, [username]);

        if (existingUser) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Insert new user into database
        const insertQuery = 'INSERT INTO users (username, password_hash) VALUES (?, ?)';
        await db.promise().query(insertQuery, [username, hashedPassword]);

        res.status(201).json({ message: "Registration successful!" });

    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({ 
            error: "Internal server error during registration",
            details: process.env.NODE_ENV === 'development' ? error.message : undefined
        });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
