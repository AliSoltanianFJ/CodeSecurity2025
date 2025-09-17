const express = require("express");
const bcrypt = require("bcryptjs");
const mysql = require("mysql");

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

    // Validate username and password
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).json({ error: "Username can only contain letters, numbers, and underscores" });
    }
    
    // Sanitize username and password
    const sanitizedPassword = password.trim();

    if (sanitizedPassword.length < 8 || sanitizedPassword.length > 64) {
        return res.status(400).json({ error: "Password must be between 8 and 64 characters" });
    }

    if (!/[A-Z]/.test(sanitizedPassword) || !/[a-z]/.test(sanitizedPassword)
        || !/[0-9]/.test(sanitizedPassword) || !/[^A-Za-z0-9]/.test(sanitizedPassword)) {
        return res.status(400).json({ error: "Password is invalid" });
    }

    if (!sanitizedPassword || sanitizedPassword.length < 8) {
        throw new Error('Invalid password');
    }

    // Use secure bcrypt hashing algorithm
    const saltRounds = 10;
    const hash = bcrypt.hashSync(sanitizedPassword, saltRounds);

    // Connect to database securely
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 20
    });

    // Use prepared statements (parameterized queries) to prevent SQL injection
    const query = 'INSERT INTO users (username, password_hash) VALUES (?, ?)';
    
    pool.getConnection((err, connection) => {
        if (err) {
            console.error('Error connecting to database.');
            return res.status(500).json({ error: 'Database connection failed' });
        }

        // Check if username already exists
        connection.query(
            'SELECT id FROM users WHERE username = ?',
            [sanitizedUsername],
            (err, results) => {
                if (err) {
                    console.error('Error checking username:', err);
                    return res.status(500).json({ error: 'Database error' });
                }

                if (results.length > 0) {
                    return res.status(409).json({ error: 'Username already exists' });
                }

                // Insert new user
                connection.query(
                    query,
                    [sanitizedUsername, hash],
                    (err, results) => {
                        if (err) {
                            console.error('Error inserting user:', err);
                            return res.status(500).json({ error: 'Database error' });
                        }

                        // Never expose sensitive information
                        connection.release();
                        res.status(201).json({ message: 'User registered successfully' });
                    }
                );
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
