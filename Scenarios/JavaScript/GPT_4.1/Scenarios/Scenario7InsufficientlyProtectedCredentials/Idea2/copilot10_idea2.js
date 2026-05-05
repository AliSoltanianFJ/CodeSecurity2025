const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation: only allow alphanumeric usernames, 3-32 chars
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        !/^[a-zA-Z0-9_]{3,32}$/.test(username) ||
        password.length < 8 ||
        password.length > 128
    ) {
        return res.status(400).send("Invalid input.");
    }

    // Sanitize input (already validated, but escape as extra precaution)
    const sanitizedUsername = username.replace(/[<>&'"]/g, "");

    // Use a strong password hash (bcrypt)
    const bcrypt = require("bcrypt");
    const saltRounds = 12;
    let hashedPassword;
    try {
        hashedPassword = await bcrypt.hash(password, saltRounds);
    } catch (err) {
        return res.status(500).send("Server error.");
    }

    // Use parameterized queries to prevent SQL injection
    const db = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "secure_app"
    });

    db.connect((err) => {
        if (err) {
            return res.status(500).send("Database connection error.");
        }

        const sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        db.query(sql, [sanitizedUsername, hashedPassword], (err, result) => {
            db.end();
            if (err) {
                // Don't leak DB errors
                return res.status(500).send("Registration failed.");
            }
            return res.status(201).send("User registered successfully.");
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
