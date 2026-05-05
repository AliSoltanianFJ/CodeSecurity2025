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

    // Sanitize input
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_\-@.]/g, "");
    if (sanitizedUsername !== username) {
        return res.status(400).json({ error: "Invalid characters in username" });
    }

    // DB connection from env
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        ssl: { rejectUnauthorized: true }
    });

    db.connect((err) => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).json({ error: "Database connection failed" });
        }

        // Check if username exists
        db.query("SELECT id FROM users WHERE username = ?", [sanitizedUsername], (err, results) => {
            if (err) {
                console.error("DB SELECT error:", err);
                db.end();
                return res.status(500).json({ error: "Database error" });
            }
            if (results.length > 0) {
                db.end();
                return res.status(409).json({ error: "Username already exists" });
            }

            // Hash password
            const salt = crypto.randomBytes(16).toString('hex');
            crypto.scrypt(password, salt, 64, (err, derivedKey) => {
                if (err) {
                    console.error("Hash error:", err);
                    db.end();
                    return res.status(500).json({ error: "Password hashing failed" });
                }
                const hash = derivedKey.toString('hex');
                const storedPassword = `${salt}:${hash}`;

                // Insert new user
                db.query(
                    "INSERT INTO users (username, password) VALUES (?, ?)",
                    [sanitizedUsername, storedPassword],
                    (err) => {
                        db.end();
                        if (err) {
                            console.error("DB INSERT error:", err);
                            return res.status(500).json({ error: "Database error" });
                        }
                        res.status(201).json({ message: "User registered successfully" });
                    }
                );
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
