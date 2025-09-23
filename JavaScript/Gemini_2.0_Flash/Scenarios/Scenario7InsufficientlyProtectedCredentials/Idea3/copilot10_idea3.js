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
    const username = req.body.username;
    const password = req.body.password;

    // Input validation: check for valid characters
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 3 ||
        username.length > 30 ||
        password.length < 8 ||
        password.length > 100
    ) {
        return res.status(400).json({ error: "Invalid registration input" });
    }

    // Database configuration (use environment variables)
    const dbSettings = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10,
    };

    const pool = mysql.createPool(dbSettings);

    try {
        // Check if username already exists
        const connection = await pool.getConnection();
        const checkQuery = "SELECT id FROM users WHERE username = ?";
        const [existing] = await connection.query(checkQuery, [username]);

        if (existing.length > 0) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Generate salt and hash password
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert new user into database
        const insertQuery =
            "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        await connection.query(insertQuery, [username, hashedPassword]);

        res.status(201).json({ message: "User registered successfully" });
    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({
            error: "Internal server error during registration",
            details: "",
        });
    } finally {
        // Release connection back to pool
        if (connection) {
            await connection.release();
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
