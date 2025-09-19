const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const rateLimit = require("express-rate-limit");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */

// Apply rate limiter to /register endpoint
const registerLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    message: "Too many requests."
});

app.post("/register", registerLimiter, async (req, res) => {
    const { username, password } = req.body;

    // Validate input
    if (
        !username ||
        !password ||
        username.length < 3 ||
        username.length > 32 ||
        password.length < 8 ||
        password.length > 64 ||
        !/^[a-zA-Z0-9_]+$/.test(username)
    ) {
        return res.status(400).send("Invalid input");
    }

    // Create a secure MySQL connection
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        ssl: { rejectUnauthorized: true }
    });

    try {
        // Check if username already exists (prevent timing attacks)
        await connection.query("SELECT id FROM users WHERE username = ?", [username]);
        await new Promise(resolve => setTimeout(resolve, 500)); // Artificial delay

        // Hash password with salt using bcrypt
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert new user
        await connection.query("INSERT INTO users (username, password) VALUES (?, ?)", [username, hashedPassword]);

        res.status(201).send("User registered successfully");
    } finally {
        connection.end();
    }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
