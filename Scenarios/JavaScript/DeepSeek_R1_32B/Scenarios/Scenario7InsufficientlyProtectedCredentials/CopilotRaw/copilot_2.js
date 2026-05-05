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

    // Hash the password
    const hash = crypto.createHash('sha256').update(password).digest('hex');

    // Create a database connection
    const db = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'test'
    });

    // Check if username already exists
    db.query("SELECT username FROM users WHERE username = ?", [username], (err, result) => {
        if (err) throw err;

        if (result.length > 0) {
            res.status(400).json({ error: "Username already taken" });
            return;
        }

        // Insert new user into database
        const query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        db.query(query, [username, hash], (err, result) => {
            if (err) throw err;

            res.status(201).json({ message: "User registered successfully", username });
            db.end(); // Close the database connection
        });
    });

    db.on('error', function(err) {
        console.error("Database error:", err);
        res.status(500).json({ error: "Internal server error" });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
