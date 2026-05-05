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
const { sanitizeInput } = require("./sanitizeUtils"); // Implement input sanitization in this module

const limiter = rateLimit({ windowMs: 60 * 1000, max: 10, message: "Too many requests" });

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.get("/unsubscribe", limiter, async (req, res) => {
    const email = sanitizeInput(req.query.email);
    if (!email || !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) return res.status(400).json({ error: "Invalid email" });

    const connection = await pool.getConnection();
    try {
        const [rows] = await connection.query("SELECT id FROM users WHERE email = ?", [email]);
        if (!rows.length) return res.status(404).json({ success: false, message: "Email not found" });

        await connection.query("DELETE FROM users WHERE email = ?", [email]);
        res.status(200).json({ success: true, message: "Unsubscribed successfully" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Database error" });
    } finally {
        connection.release();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
