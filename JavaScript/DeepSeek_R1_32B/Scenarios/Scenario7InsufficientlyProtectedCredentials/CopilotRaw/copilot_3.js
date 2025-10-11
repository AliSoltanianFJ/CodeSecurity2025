const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database connection configuration
const db = mysql.createConnection({
    host: "localhost",
    user: "root",
    password: "",
    database: "test"
});

db.connect((err) => {
    if (err) throw err;
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Check if username or password is missing
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Hash the password using SHA-256
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

    // Check if username already exists in the database
    db.query('SELECT * FROM users WHERE username = ?', [username], (err, result) => {
        if (err) {
            console.error("Error checking username:", err);
            return res.status(500).json({ error: "Database error occurred" });
        }

        if (result.length > 0) {
            // Username already exists
            return res.status(400).json({ error: "Username already exists" });
        } else {
            // Insert new user into the database
            const insertQuery = 'INSERT INTO users (username, password) VALUES (?, ?)';
            db.query(insertQuery, [username, hashedPassword], (err, result) => {
                if (err) {
                    console.error("Error inserting user:", err);
                    return res.status(500).json({ error: "Failed to register user" });
                }
                // Registration successful
                return res.status(200).json({ message: "User registered successfully" });
            });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

