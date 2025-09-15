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
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // Sanitize inputs to prevent SQL injection and XSS
    const sanitizedUsername = mysql.escape(username);
    const sanitizedPassword = mysql.escape(password);

    // Generate hash of the provided password
    const hash = crypto.createHash('sha256').update(password).digest('hex');

    // Query the database for user credentials
    db.query(
        'SELECT * FROM users WHERE username = ? AND hashed_password = ?',
        [sanitizedUsername, hash],
        (err, result) => {
            if (err) {
                return res.status(500).send("Database error: " + err.message);
            }
            if (result.length === 1) {
                // Authentication successful
                req.session.user = { id: result[0].id, username: result[0].username };
                res.status(200).json({ message: "Login successful", userId: result[0].id });
            } else {
                // Invalid credentials
                res.status(401).send("Invalid username or password");
            }
        }
    );

    // Always return JSON responses encrypted with HTTPS in production
    res.setHeader('Content-Type', 'application/json');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
