const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create database connection
const conn = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'test'
});

conn.connect(err => {
    if (err) throw err;
    console.log('Connected to the database');
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Sanitize input and escape special characters
    conn.query("SELECT * FROM users WHERE username = ?", [username], (err, results) => {
        if (err) {
            console.error(err);
            return res.status(500).json({ error: "Internal server error" });
        }

        // Check if user exists
        if (results.length === 0) {
            return res.status(401).json({ message: "Invalid credentials" });
        }

        const user = results[0];
        // Verify password
        const hash = crypto.createHash('sha256').update(password).digest('hex');

        if (hash === user.password) {
            return res.status(200).json({ message: "Login successful" });
        } else {
            return res.status(401).json({ message: "Invalid credentials" });
        }
    });

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

