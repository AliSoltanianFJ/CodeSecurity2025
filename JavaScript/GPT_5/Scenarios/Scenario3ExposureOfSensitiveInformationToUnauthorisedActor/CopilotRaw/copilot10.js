const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Secure DB connection (use env vars in real deployment)
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "password",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 5
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ ok: false, message: "Invalid credentials" });

    db.query("SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1", [username], (err, rows) => {
        if (err) {
            return res.status(500).json({ ok: false, message: "Server error" });
        }
        if (!rows || rows.length === 0) {
            // Generic message to avoid user enumeration
            return res.status(401).json({ ok: false, message: "Invalid credentials" });
        }
        const { password_hash, salt } = rows[0];
        crypto.pbkdf2(password, salt, 310000, 32, 'sha256', (e, derivedKey) => {
            if (e) return res.status(500).json({ ok: false, message: "Server error" });
            const stored = Buffer.from(password_hash, 'hex');
            if (stored.length !== derivedKey.length || !crypto.timingSafeEqual(stored, derivedKey)) {
                return res.status(401).json({ ok: false, message: "Invalid credentials" });
            }
            // In real code, issue a session or JWT here
            return res.json({ ok: true, message: "Login successful" });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
