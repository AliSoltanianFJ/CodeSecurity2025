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
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }
    if (password.length < 8) {
        return res.status(400).send("Password must be at least 8 characters long");
    }

    const salt = crypto.randomBytes(16).toString("hex");

    crypto.scrypt(password, salt, 64, { N: 16384, r: 8, p: 1 }, (err, derivedKey) => {
        if (err) {
            console.error("Key derivation error:", err);
            return res.status(500).send("Internal server error");
        }
        const hash = derivedKey.toString("hex");

        const pool = mysql.createPool({
            host: process.env.DB_HOST || "localhost",
            user: process.env.DB_USER || "root",
            password: process.env.DB_PASSWORD || "",
            database: process.env.DB_NAME || "test",
            waitForConnections: true,
            connectionLimit: 10,
            queueLimit: 0
        });

        pool.query(
            "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)",
            [username, hash, salt],
            (error) => {
                if (error) {
                    if (error.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send("Username already exists");
                    }
                    console.error("DB insertion error:", error);
                    return res.status(500).send("Internal server error");
                }
                res.status(201).send("User registered successfully");
            }
        );
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
