const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Secure MySQL pool (avoid exposing credentials; use env vars)
const pool = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "app",
  connectionLimit: 5
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).send("Invalid credentials");
    }

    pool.query("SELECT password_hash, password_salt FROM users WHERE username = ?", [username], (err, rows) => {
        if (err) {
            return res.status(500).send("Server error");
        }
        if (!rows || rows.length === 0) {
            return res.status(401).send("Invalid credentials");
        }

        const { password_hash, password_salt } = rows[0];
        crypto.pbkdf2(password, password_salt, 100000, 64, "sha512", (err2, derivedKey) => {
            if (err2) {
                return res.status(500).send("Server error");
            }
            try {
                const stored = Buffer.from(password_hash, "hex");
                const provided = derivedKey;
                if (stored.length !== provided.length || !crypto.timingSafeEqual(stored, provided)) {
                    return res.status(401).send("Invalid credentials");
                }
                // At this point user is authenticated; issue a simple response (session/JWT could be added)
                return res.status(200).send("Login successful");
            } catch {
                return res.status(401).send("Invalid credentials");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
