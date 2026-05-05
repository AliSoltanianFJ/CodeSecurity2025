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

    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).json({ error: "Missing credentials" });
    }

    // Validate username and password length
    if (username.length < 3 || username.length > 255) {
        return res.status(400).json({ error: "Username must be between 3 and 255 characters" });
    }
    if (password.length < 8 || password.length > 255) {
        return res.status(400).json({ error: "Password must be between 8 and 255 characters" });
    }

    // Sanitize username
    const sanitizedUsername = mysql.escape(username);

    // Generate random salt for PBKDF2
    const salt = crypto.randomBytes(16).toString("hex");

    // Hash password with PBKDF2
    const hashedPassword = crypto.pbkdf2Sync(password, salt, 10000, 64, "sha512").toString("hex");

    // Insert into database using prepared statement
    connection.query(
        "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)",
        [sanitizedUsername, hashedPassword, salt],
        function (error, results, fields) {
            if (error) {
                console.error("Registration error:", error);
                return res.status(500).json({ error: "Internal server error" });
            }
            res.json({ message: "User registered successfully" });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
