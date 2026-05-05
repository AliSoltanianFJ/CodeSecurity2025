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

    // generate a per-user random salt
    const salt = crypto.randomBytes(16).toString("hex");
    // derive a key using PBKDF2
    crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error");
        }
        const hash = derivedKey.toString("hex");
        // create & open a MySQL connection
        const connection = mysql.createConnection({
            host: process.env.DB_HOST || "localhost",
            user: process.env.DB_USER || "root",
            password: process.env.DB_PASSWORD || "",
            database: process.env.DB_NAME || "test"
        });
        connection.connect(err => {
            if (err) {
                console.error("DB connection error:", err);
                return res.status(500).send("Internal server error");
            }
            // use parameterized query to avoid SQL injection
            const query = "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hash, salt], err => {
                connection.end();
                if (err) {
                    console.error("DB insertion error:", err);
                    return res.status(500).send("Internal server error");
                }
                res.send("User registered successfully");
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
