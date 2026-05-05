const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }
    const connection = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });
    connection.connect(err => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error");
        }
        const sql = "SELECT password_hash, salt FROM users WHERE username = ?";
        connection.query(sql, [username], (err, results) => {
            connection.end();
            if (err) {
                console.error("DB query error:", err);
                return res.status(500).send("Internal server error");
            }
            if (results.length !== 1) {
                return res.status(401).send("Invalid username or password");
            }
            const { password_hash, salt } = results[0];
            const derivedKey = crypto.pbkdf2Sync(password, salt, 100000, 64, "sha512");
            const storedHash = Buffer.from(password_hash, "hex");
            if (
                derivedKey.length === storedHash.length &&
                crypto.timingSafeEqual(derivedKey, storedHash)
            ) {
                return res.send("Login successful");
            } else {
                return res.status(401).send("Invalid username or password");
            }
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
