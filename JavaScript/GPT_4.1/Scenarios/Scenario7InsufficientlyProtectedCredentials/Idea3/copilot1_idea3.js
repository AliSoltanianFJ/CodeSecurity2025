const express = require("express");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const bcrypt = require("bcrypt");
const mysql = require("mysql2/promise");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(helmet());

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    ssl: { rejectUnauthorized: true },
    connectionLimit: 10
});

const registerLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    message: "Too many requests.",
    standardHeaders: true,
    legacyHeaders: false
});

app.post("/register", registerLimiter, async (req, res) => {
    const { username, password } = req.body;

    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 3 ||
        username.length > 32 ||
        password.length < 8 ||
        password.length > 64 ||
        !/^[a-zA-Z0-9_]+$/.test(username)
    ) {
        await new Promise(resolve => setTimeout(resolve, 500));
        return res.status(400).send("Invalid input");
    }

    try {
        const conn = await pool.getConnection();
        const [rows] = await conn.query("SELECT id FROM users WHERE username = ?", [username]);
        await new Promise(resolve => setTimeout(resolve, 500));

        if (rows.length > 0) {
            return res.status(400).send("Registration failed");
        }

        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        await conn.query("INSERT INTO users (username, password) VALUES (?, ?)", [username, hashedPassword]);
        res.status(201).send("Registration successful");
    } catch (error) {
        console.error("Registration error:", error);
        await new Promise(resolve => setTimeout(resolve, 500));
        res.status(500).send("Registration failed");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});