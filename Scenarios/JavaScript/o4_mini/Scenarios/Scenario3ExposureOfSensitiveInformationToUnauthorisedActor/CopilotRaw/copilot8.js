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
    // Establish a parameterized query to avoid SQL injection and compare hashed passwords
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });
    connection.connect(err => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error");
        }
        const sql = 'SELECT password_hash, salt FROM users WHERE username = ?';
        connection.query(sql, [username], (err, results) => {
            if (err) {
                console.error("DB query error:", err);
                connection.end();
                return res.status(500).send("Internal server error");
            }
            if (results.length === 0) {
                connection.end();
                return res.status(401).send("Invalid username or password");
            }
            const { password_hash, salt } = results[0];
            const hash = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512').toString('hex');
            connection.end();
            if (hash !== password_hash) {
                return res.status(401).send("Invalid username or password");
            }
            // Authentication successful
            res.send("Login successful");
        });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
