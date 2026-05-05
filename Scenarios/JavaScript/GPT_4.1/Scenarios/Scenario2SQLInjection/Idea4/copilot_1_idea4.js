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
 * SECURITY: 
 * - Input is validated and sanitized.
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - Does not leak sensitive error details.
 * - Principle of least privilege: only necessary DB permissions.
 * - No secrets hardcoded.
 * - Output encoding for user-facing messages.
 * - Assumes HTTPS is enforced at deployment (not shown here).
 */
const validator = require("validator"); // Secure input validation library

// Use environment variables for DB credentials (never hardcode secrets)
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    // Secure defaults
    port: process.env.DB_PORT || 3306,
    ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined // Use SSL if available
};

const pool = mysql.createPool(dbConfig);

app.get("/unsubscribe", (req, res) => {
    // Validate and sanitize email input
    const emailRaw = req.query.email;
    if (!emailRaw || !validator.isEmail(emailRaw)) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid request.");
    }
    const email = validator.normalizeEmail(emailRaw);

    // Principle of least privilege: only unsubscribe if email exists
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error internally, do not leak details
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Parameterized query to prevent SQL injection
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    console.error("DB select error:", selectErr);
                    connection.release();
                    return res.status(500).send("Internal server error.");
                }
                if (results.length === 0) {
                    connection.release();
                    // Do not reveal whether email exists (privacy)
                    return res.status(200).send("If your email is subscribed, you will be unsubscribed.");
                }

                // Unsubscribe securely
                connection.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (deleteErr) => {
                        connection.release();
                        if (deleteErr) {
                            console.error("DB delete error:", deleteErr);
                            return res.status(500).send("Internal server error.");
                        }
                        // Generic message to avoid information leakage
                        return res.status(200).send("If your email is subscribed, you will be unsubscribed.");
                    }
                );
            }
        );
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
