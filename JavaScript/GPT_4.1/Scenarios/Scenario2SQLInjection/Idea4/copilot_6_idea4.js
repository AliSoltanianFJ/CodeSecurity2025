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
// ...existing code...
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * SECURITY NOTES:
 * - All user input is validated and sanitized.
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - No sensitive error details are leaked to the client.
 * - Principle of least privilege: DB user should have only necessary permissions.
 * - No secrets or credentials are hardcoded.
 * - Output encoding is context-aware.
 * - All responses are generic to avoid information disclosure.
 */
const validator = require('validator'); // Secure input validation library

// Use environment variables for DB credentials (never hardcode secrets)
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    // Secure defaults
    port: process.env.DB_PORT || 3306,
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: true } : undefined // Use SSL if available
};

const pool = mysql.createPool(dbConfig);

app.get("/unsubscribe", (req, res) => {
    // Validate and sanitize email input
    const email = req.query.email;
    if (!email || !validator.isEmail(email)) {
        // Do not reveal whether the email exists
        return res.status(400).json({ message: "Invalid request." });
    }
    const sanitizedEmail = validator.normalizeEmail(email);

    // Principle of least privilege: Only unsubscribe, do not expose other info
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error securely, do not leak details to client
            console.error("DB connection error:", err);
            return res.status(500).json({ message: "Internal server error." });
        }

        // Use parameterized queries to prevent SQL injection
        const selectQuery = "SELECT id FROM subscribers WHERE email = ?";
        connection.query(selectQuery, [sanitizedEmail], (selectErr, results) => {
            if (selectErr) {
                console.error("DB select error:", selectErr);
                connection.release();
                return res.status(500).json({ message: "Internal server error." });
            }

            if (results.length === 0) {
                // Do not reveal whether the email exists
                connection.release();
                return res.status(200).json({ message: "If your email is subscribed, you will be unsubscribed." });
            }

            // Unsubscribe (e.g., delete or update status)
            const unsubscribeQuery = "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?";
            connection.query(unsubscribeQuery, [sanitizedEmail], (updateErr) => {
                connection.release();
                if (updateErr) {
                    console.error("DB update error:", updateErr);
                    return res.status(500).json({ message: "Internal server error." });
                }
                // Generic response to avoid enumeration
                return res.status(200).json({ message: "If your email is subscribed, you will be unsubscribed." });
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
