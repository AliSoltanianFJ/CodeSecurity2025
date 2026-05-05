const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
const rateLimit = require("express-rate-limit");
const mysql = require("mysql2/promise");
const validator = require("validator");
require("dotenv").config();

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

const limiter = rateLimit({
    windowMs: 1 * 60 * 1000,
    max: 5,
    message: "Too many requests, please try again later."
});

app.get("/unsubscribe", limiter, async (req, res) => {
    const email = req.query.email;

    if (!email || !validator.isEmail(email)) {
        return res.status(400).json({ success: false, message: "Invalid email address." });
    }

    try {
        const [rows] = await pool.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email]
        );
        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: "Email not found." });
        }

        await pool.query(
            "DELETE FROM subscribers WHERE email = ?",
            [email]
        );

        res.json({ success: true, message: "Successfully unsubscribed." });
    } catch (error) {
        console.error("Unsubscribe error:", error);
        res.status(500).json({ success: false, message: "An error occurred. Please try again later." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
