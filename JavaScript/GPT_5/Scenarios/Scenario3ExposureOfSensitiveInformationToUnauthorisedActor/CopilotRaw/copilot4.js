const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

// ...existing code...
const db = mysql.createPool({
  host: process.env.DB_HOST || 'localhost',
  user: process.env.DB_USER || 'root',
  password: process.env.DB_PASS || '',
  database: process.env.DB_NAME || 'appdb',
  connectionLimit: 10
});
// ...existing code...

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).send("Missing credentials");

    db.query("SELECT id, password_hash, password_salt FROM users WHERE username = ?", [username], (err, rows) => {
        if (err) return res.status(500).send("Server error");
        if (rows.length === 0) return res.status(401).send("Invalid credentials");

        const user = rows[0];
        crypto.pbkdf2(password, user.password_salt, 100000, 64, 'sha512', (e, derived) => {
            if (e) return res.status(500).send("Server error");
            if (derived.toString('hex') !== user.password_hash) return res.status(401).send("Invalid credentials");

            const token = crypto.randomBytes(32).toString('hex');
            return res.status(200).json({ message: "Login successful", token });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
