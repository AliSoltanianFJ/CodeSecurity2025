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
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    // Validate email format using a strict regex (RFC 5322 Official Standard)
    const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
    if (!email || typeof email !== "string" || !emailRegex.test(email)) {
        // Do not reveal if the email exists or not
        return res.status(400).json({ message: "Invalid request." });
    }

    // Use environment variables for DB credentials (never hardcode secrets)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        // Secure defaults
        port: process.env.DB_PORT || 3306,
        ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
        multipleStatements: false // Prevent statement chaining
    };

    const connection = mysql.createConnection(dbConfig);

    // Principle of least privilege: DB user should only have SELECT/UPDATE on relevant tables

    // Use parameterized queries to prevent SQL injection
    connection.connect((err) => {
        if (err) {
            // Log error securely (not shown to user)
            console.error("DB connection error:", err);
            return res.status(500).json({ message: "Internal server error." });
        }

        // Check if email exists
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    console.error("DB select error:", selectErr);
                    connection.end();
                    return res.status(500).json({ message: "Internal server error." });
                }

                if (results.length === 0) {
                    // Do not reveal if the email exists or not
                    connection.end();
                    return res.status(200).json({ message: "If this email is subscribed, it will be unsubscribed." });
                }

                // Unsubscribe (soft delete or update status, not hard delete for auditability)
                connection.query(
                    "UPDATE subscribers SET unsubscribed = 1, unsubscribed_at = NOW() WHERE id = ?",
                    [results[0].id],
                    (updateErr) => {
                        connection.end();
                        if (updateErr) {
                            console.error("DB update error:", updateErr);
                            return res.status(500).json({ message: "Internal server error." });
                        }
                        // Generic response to avoid leaking info
                        return res.status(200).json({ message: "If this email is subscribed, it will be unsubscribed." });
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
