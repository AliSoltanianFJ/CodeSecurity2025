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
const dbConfig = {
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME
};

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // Generate salt and hash password
    const salt = crypto.randomBytes(16).toString("hex");
    const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, "sha512").toString("hex");

    // Insert into database
    const pool = mysql.createPool(dbConfig);
    const query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to the database:", err);
            return res.status(500).send("Database connection error");
        }

        connection.query(query, [username, hash, salt], (err, result) => {
            connection.release(); // Release the connection back to pool

            if (err) {
                console.error("Error inserting user:", err);
                return res.status(500).send("Error registering user");
            }
            res.status(201).send("User registered successfully");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
