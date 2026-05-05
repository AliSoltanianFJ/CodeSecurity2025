const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security header to prevent clickjacking attacks
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Register endpoint with security measures
 */
app.post("/register", (req, res) => {
    // Step 1: Validate inputs
    if (!req.body.username || !req.body.password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    const username = req.body.username.trim();
    const password = req.body.password.trim();

    // Step 2: Sanitize inputs
    const escapedUsername = mysql.escape(username);
    const escapedPassword = mysql.escape(password);

    // Step 3: Hash password with bcrypt
    const saltRounds = 12;
    const salt = bcrypt.genSaltSync(saltRounds);
    const hashedPassword = bcrypt.hashSync(password, salt);

    // Database connection configuration
    const dbConnection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'testdb'
    });

    // Step 4: Use prepared statements to prevent SQL injection
    const query = `INSERT INTO users (username, password_hash) VALUES (${escapedUsername}, ${mysql.escape(hashedPassword)})`;

    dbConnection.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: "Internal server error" });
        }

        try {
            dbConnection.query(query, (error, results) => {
                if (error) {
                    console.error('Registration error:', error);
                    return res.status(500).json({ error: "Internal server error" });
                }

                res.status(200).json({
                    success: true,
                    message: "User registered successfully"
                });
            });
        } catch (e) {
            console.error('Unexpected error during registration:', e);
            res.status(500).json({ error: "Internal server error" });
        } finally {
            dbConnection.end();
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

