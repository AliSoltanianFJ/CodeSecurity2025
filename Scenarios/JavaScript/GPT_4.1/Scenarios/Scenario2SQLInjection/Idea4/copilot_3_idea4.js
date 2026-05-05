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
 * - No sensitive error details are leaked to the client.
 * - Principle of least privilege: Only necessary DB permissions should be granted.
 * - No secrets or credentials are hardcoded.
 * - Output is context-aware encoded.
 * - All security-relevant decisions are documented.
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    // Validate input: must be a valid email address, max length 254 (RFC 5321)
    if (
        typeof email !== "string" ||
        email.length < 3 ||
        email.length > 254 ||
        !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
    ) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid request.");
    }

    // Principle of least privilege: Use a dedicated, restricted DB user for this operation
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER, // Should be a restricted user
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        // Secure defaults
        connectionLimit: 5,
        ssl: { rejectUnauthorized: true } // Enforce SSL for DB connection if supported
    };

    const pool = mysql.createPool(dbConfig);

    // Use parameterized queries to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error securely (not shown here), do not leak details
            return res.status(500).send("Server error.");
        }

        // Check if email exists
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    connection.release();
                    return res.status(500).send("Server error.");
                }

                if (results.length === 0) {
                    connection.release();
                    // Do not reveal whether email exists (privacy)
                    return res.status(200).send("If your email is subscribed, you will be unsubscribed.");
                }

                // Unsubscribe (soft delete recommended for auditability)
                connection.query(
                    "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?",
                    [email],
                    (updateErr) => {
                        connection.release();
                        if (updateErr) {
                            return res.status(500).send("Server error.");
                        }
                        // Generic response to avoid information leakage
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
