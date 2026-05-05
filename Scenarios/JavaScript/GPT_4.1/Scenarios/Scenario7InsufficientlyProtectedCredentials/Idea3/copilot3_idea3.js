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
    if (typeof username !== "string" || typeof password !== "string" || username.trim() === "" || password.trim() === "") {
        return res.status(400).send("Invalid username or password");
    }

    // Use environment variable for DB credentials
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    // Sanitize input and use parameterized query
    const salt = crypto.randomBytes(16).toString('hex');
    const hashedPassword = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');

    pool.getConnection((err, connection) => {
        if (err) {
            return res.status(500).send("Database connection error");
        }
        connection.query(
            "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)",
            [username, hashedPassword, salt],
            (error, results) => {
                connection.release();
                if (error) {
                    if (error.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send("Username already exists");
                    }
                    return res.status(500).send("Registration failed");
                }
                res.status(201).send("User registered successfully");
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
