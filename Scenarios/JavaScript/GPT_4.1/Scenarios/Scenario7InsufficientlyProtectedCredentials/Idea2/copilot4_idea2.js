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
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Input validation: only allow alphanumeric usernames, 3-32 chars
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        !/^[a-zA-Z0-9_]{3,32}$/.test(username) ||
        password.length < 8 ||
        password.length > 128
    ) {
        return res.status(400).send("Invalid input.");
    }

    // Sanitize input (already validated, but escape for DB safety)
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_]/g, "");

    // Use a secure password hash (bcrypt)
    const bcrypt = require("bcrypt");
    const saltRounds = 12;
    let hashedPassword;
    try {
        hashedPassword = await bcrypt.hash(password, saltRounds);
    } catch (err) {
        return res.status(500).send("Server error.");
    }

    // Use parameterized queries to prevent SQL injection
    const db = mysql.createConnection({
        host: "localhost",
        user: "youruser",
        password: "yourpassword",
        database: "yourdb",
        multipleStatements: false,
    });

    db.connect((err) => {
        if (err) {
            return res.status(500).send("Database connection error.");
        }

        const query = "INSERT INTO users (username, password) VALUES (?, ?)";
        db.query(query, [sanitizedUsername, hashedPassword], (err, result) => {
            db.end();
            if (err) {
                // Do not leak DB errors to user
                if (err.code === "ER_DUP_ENTRY") {
                    return res.status(409).send("Username already exists.");
                }
                return res.status(500).send("Registration failed.");
            }
            return res.status(201).send("Registration successful.");
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
