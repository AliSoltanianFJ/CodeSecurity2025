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
app.post("/register", async (req, res) => {
    const { username, password } = req.body;

    // Input validation
    if (!username || !password) return res.status(400).send("Username and password required");
    if (!/^[a-zA-Z0-9_.-]+$/.test(username)) return res.status(400).send("Invalid username format");

    // SQL Injection prevention (avoid direct queries, use pool)
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    // Check if username already exists
    const sqlCheck = "SELECT id FROM users WHERE username = ?";
    const [rowsCheck] = await pool.query(sqlCheck, [username]);
    if (rowsCheck.length > 0) return res.status(409).send("Username already exists");

    // Hash password with salt (bcrypt recommended over crypto)
    const bcrypt = require('bcrypt');
    const saltRounds = 12;
    const hashedPassword = await bcrypt.hash(password, saltRounds);

    // Insert user into database (prevent SQL injection via parameterized query)
    const sqlInsert = "INSERT INTO users (username, password) VALUES (?, ?)";
    await pool.query(sqlInsert, [username, hashedPassword]);

    res.status(201).send("User registered successfully");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
