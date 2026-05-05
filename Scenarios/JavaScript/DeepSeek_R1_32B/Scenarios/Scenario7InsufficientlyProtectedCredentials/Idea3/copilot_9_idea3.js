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
    // Validate input length and format
    const username = req.body.username?.trim();
    const password = req.body.password;

    if (!username || !password ||
        username.length < 3 || username.length > 32 ||
        password.length < 8 || password.length > 100) {
        return res.status(400).send('Invalid input');
    }

    // Only allow alphanumeric usernames
    if (!/^[a-zA-Z0-9]+$/.test(username)) {
        return res.status(400).send('Invalid username');
    }

    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });

    db.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).send('Internal server error');
        }
    });

    // Check if username already exists
    const checkQuery = 'SELECT id FROM users WHERE username = ?';
    db.query(checkQuery, [username], (err, results) => {
        if (err) {
            console.error('Error checking username:', err);
            return res.status(500).send('Internal server error');
        }

        if (results.length > 0) {
            return res.status(409).send('Username already exists');
        }

        // Hash password securely using PBKDF2
        const salt = crypto.randomBytes(16).toString('hex');
        const hash = crypto.pbkdf2Sync(password, salt, 100000, 32, 'sha256').toString('hex');

        // Insert new user into database
        const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        db.query(insertQuery, [username, hash, salt], (err, results) => {
            if (err) {
                console.error('Error inserting user:', err);
                return res.status(500).send('Internal server error');
            }

            // Never expose sensitive information in response
            return res.status(200).send('Registration successful');
        });
    });

    db.end();
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
