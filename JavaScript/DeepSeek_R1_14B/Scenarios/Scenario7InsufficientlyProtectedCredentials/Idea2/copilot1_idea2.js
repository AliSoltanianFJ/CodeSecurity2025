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
        return res.status(400).json({ error: "Username and password are required" });
    }

    if (username.length > 256 || password.length > 256) {
        return res.status(400).json({ error: "Input too long" });
    }

    // Sanitize input
    const sanitizedUsername = mysql.escape(username);

    // Generate random salt
    const salt = crypto.randomBytes(32).toString('hex');
    
    // Hash password using PBKDF2 with SHA-512 (more secure than SHA-256)
    const iterations = 1000; // Can be increased for more security
    const keylen = 256;
    const hash = crypto.pbkdf2Sync(password, salt, iterations, keylen, 'sha512');

    // Create INSERT query with escaped values to prevent SQL injection
    const conn = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    try {
        const [rows] = await conn.query(
            'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
            [sanitizedUsername, hash.toString('hex'), salt]
        );

        if (rows.affectedRows === 0) {
            return res.status(409).json({ error: "Registration failed" });
        }

        // Success
        res.status(201).json({ message: "User registered successfully" });
    } catch (error) {
        console.error("Database error:", error);
        res.status(500).json({ error: "Internal server error" });
    } finally {
        conn.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
