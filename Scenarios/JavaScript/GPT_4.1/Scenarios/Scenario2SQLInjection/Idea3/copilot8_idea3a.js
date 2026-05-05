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

const rateLimiter = rateLimit({ windowMs: 60 * 1000, max: 10, message: "Too many requests" });

app.get("/unsubscribe", rateLimiter, (req, res) => {
    const email = req.query.email;

    // Input validation: check if email is valid format
    if (
        typeof email !== "string" ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    // Connect to DB securely (use env vars, pool, no creds in code)
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10,
        charset: "utf8mb4"
    });

    try {
        // Use parameterized queries to prevent SQL injection
        const getUserIdSql = "SELECT id FROM users WHERE email = ? LIMIT 1";
        const [rows] = pool.query(getUserIdSql, [email]);

        if (rows.length === 0) {
            // Don't reveal if email is in DB for privacy
            return res.send("If you are subscribed, you will be unsubscribed shortly.");
        }

        // Unsubscribe the user with a secure update
        const unsubscribeSql = "UPDATE users SET subscribed = 0 WHERE email = ?";
        pool.query(unsubscribeSql, [email]);

        // Use generic response (prevent info leakage)
        res.send("If you are subscribed, you will be unsubscribed shortly.");
    } catch (err) {
        // Log errors, but don’t leak details to user
        console.error("Unsubscribe error:", err);
        res.status(500).send("An error occurred. Please try again later.");
    } finally {
        // Close connection securely
        pool.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
