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

    // Input validation and sanitization
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 3 ||
        username.length > 32 ||
        password.length < 8 ||
        password.length > 128 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username)
    ) {
        return res.status(400).send("Invalid input.");
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

        // Check if username already exists
        db.query("SELECT id FROM users WHERE username = ?", [username], (err, results) => {
            if (err) {
                db.end();
                return res.status(500).send("Database error.");
            }
            if (results.length > 0) {
                db.end();
                return res.status(409).send("Username already taken.");
            }

            // Securely hash the password using a strong algorithm (e.g., scrypt)
            crypto.scrypt(password, process.env.PASSWORD_SALT || "defaultSalt", 64, (err, derivedKey) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Error processing password.");
                }
                const hash = derivedKey.toString("hex");

                // Store the username and hashed password
                db.query(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                    [username, hash],
                    (err) => {
                        db.end();
                        if (err) {
                            return res.status(500).send("Database error.");
                        }
                        // Do not reveal sensitive info
                        res.status(201).send("Registration successful.");
                    }
                );
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
