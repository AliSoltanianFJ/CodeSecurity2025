const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a DB connection (adjust credentials as needed)
const db = mysql.createConnection({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb"
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).send("Missing credentials");
    }

    // Parameterized query prevents SQL injection
    db.query(
        "SELECT password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, results) => {
            if (err) {
                return res.status(500).send("Server error");
            }
            if (results.length === 0) {
                return res.status(401).send("Invalid username or password");
            }

            const { password_hash, password_salt } = results[0];

            // Derive hash from provided password using stored salt (PBKDF2)
            crypto.pbkdf2(password, password_salt, 100000, 64, "sha512", (pbkErr, derivedKey) => {
                if (pbkErr) {
                    return res.status(500).send("Server error");
                }
                const computed = derivedKey.toString("hex");
                if (crypto.timingSafeEqual(Buffer.from(computed, "hex"), Buffer.from(password_hash, "hex"))) {
                    // In real app: establish session / issue token
                    return res.status(200).send("Login successful");
                } else {
                    return res.status(401).send("Invalid username or password");
                }
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
