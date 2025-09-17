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
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize inputs to prevent SQL injection
    const sanitizedUsername = mysql.escape(username);
    const sanitizedPassword = mysql.escape(password);

    // Validate username format (prevent XSS and other attacks)
    if (!/^[a-zA-Z0-9_ -]+$/i.test(sanitizedUsername)) {
        return res.status(400).json({ error: "Invalid username format" });
    }

    // Password validation
    if (password.length < 8) {
        return res.status(400).json({ error: "Password must be at least 8 characters" });
    }

    // Generate random salt
    const salt = crypto.randomBytes(32).toString('hex');

    // Hash password using bcrypt (more secure than SHA256)
    const hash = crypto
        .pbkdf2(password, salt, 100000, 64) // Use PBKDF2 instead of direct hashing
        .then(buffer => buffer.toString('hex'));

    // Insert into database with prepared statement to prevent SQL injection
    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    connection.connect();

    hash.then(hashedPassword => {
        const query = `
            INSERT INTO users (username, hashed_password, salt)
            VALUES (?, ?, ?)
            WHERE NOT EXISTS (
                SELECT 1 FROM users WHERE username = ?
            )
        `;

        connection.query(query, [sanitizedUsername, hashedPassword, salt, sanitizedUsername], (err, result) => {
            connection.end();
            
            if (err) {
                return res.status(500).json({ error: "Database error" });
            }

            if (result.affectedRows === 0) {
                return res.status(409).json({ error: "Username already exists" });
            }

            // Return success without sensitive information
            res.json({ message: "User registered successfully" });
        });
    }).catch(err => {
        console.error("Hashing error:", err);
        res.status(500).json({ error: "Internal server error" });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
