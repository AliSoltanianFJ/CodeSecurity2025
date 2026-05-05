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
 * - No sensitive error details are leaked.
 * - Principle of least privilege: Only required DB permissions.
 * - No secrets hardcoded; use environment variables for DB credentials.
 * - Output is context-aware encoded.
 * - All user input is treated as untrusted.
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    // Validate email format strictly
    if (
        typeof email !== "string" ||
        email.length < 5 ||
        email.length > 254 ||
        !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
    ) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid request.");
    }

    // Use environment variables for DB credentials (never hardcode)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        // SECURITY: Use least privilege account, restrict access to only needed tables/columns
        connectionLimit: 5, // Limit connections to reduce attack surface
        ssl: { rejectUnauthorized: true } // If supported by DB, enforce SSL
    };

    const pool = mysql.createPool(dbConfig);

    // SECURITY: Use parameterized queries to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            // Log error internally, do not leak details
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error.");
        }

        // Check if email exists
        connection.query(
            "SELECT id FROM users WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    console.error("DB select error:", selectErr);
                    connection.release();
                    return res.status(500).send("Internal server error.");
                }

                if (results.length === 0) {
                    // Do not reveal whether email exists (privacy)
                    connection.release();
                    return res.status(200).send("If your email is subscribed, you will be unsubscribed.");
                }

                // Unsubscribe user (e.g., set 'subscribed' flag to false)
                connection.query(
                    "UPDATE users SET subscribed = 0 WHERE email = ?",
                    [email],
                    (updateErr) => {
                        connection.release();
                        if (updateErr) {
                            console.error("DB update error:", updateErr);
                            return res.status(500).send("Internal server error.");
                        }
                        // Generic message to avoid user enumeration
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
