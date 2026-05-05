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
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }
    const db = mysql.createPool({
        host:     process.env.DB_HOST,
        user:     process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });
    db.query(
        'SELECT password_hash, salt FROM users WHERE username = ?',
        [username],
        (err, results) => {
            if (err) {
                console.error('DB error:', err);
                return res.status(500).send("Internal server error");
            }
            if (results.length !== 1) {
                return res.status(401).send("Invalid username or password");
            }
            const { password_hash, salt } = results[0];
            const derivedKey = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512');
            const storedKey = Buffer.from(password_hash, 'hex');
            if (
                storedKey.length !== derivedKey.length ||
                !crypto.timingSafeEqual(storedKey, derivedKey)
            ) {
                return res.status(401).send("Invalid username or password");
            }
            res.send("Login successful");
        }
    );
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
