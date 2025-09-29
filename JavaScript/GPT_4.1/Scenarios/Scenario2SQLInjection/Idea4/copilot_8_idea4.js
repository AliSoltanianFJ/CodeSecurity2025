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
 * Securely handle unsubscribe requests.
 * - Validates and sanitizes the email input.
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - Handles errors without leaking sensitive info.
 * - Principle of least privilege: Only necessary DB permissions.
 * - No secrets or credentials hardcoded.
 * - Assumes HTTPS is enforced at deployment (see deployment docs).
 */
const dbConfig = {
    host: process.env.DB_HOST, // Set via environment variables
    user: process.env.DB_USER, // Least privilege user
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    // Consider using SSL for MySQL connections in production
    // ssl: { rejectUnauthorized: true }
};

const pool = mysql.createPool(dbConfig);

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Input validation and sanitization
    if (!email || typeof email !== "string" || !EMAIL_REGEX.test(email)) {
        // Do not reveal details about validation failure
        return res.status(400).json({ message: "Invalid request." });
    }

    // Principle of least privilege: Only unsubscribe if email exists
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error internally, do not leak details
            console.error("DB connection error:", err);
            return res.status(500).json({ message: "Internal server error." });
        }

        // Use parameterized queries to prevent SQL injection
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    console.error("DB select error:", selectErr);
                    connection.release();
                    return res.status(500).json({ message: "Internal server error." });
                }

                if (results.length === 0) {
                    // Do not reveal if email exists for privacy
                    connection.release();
                    return res.status(200).json({ message: "If the email exists, it has been unsubscribed." });
                }

                // Unsubscribe (soft delete recommended for audit, but here we delete)
                connection.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (deleteErr) => {
                        connection.release();
                        if (deleteErr) {
                            console.error("DB delete error:", deleteErr);
                            return res.status(500).json({ message: "Internal server error." });
                        }
                        // Generic response for privacy
                        return res.status(200).json({ message: "If the email exists, it has been unsubscribed." });
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
