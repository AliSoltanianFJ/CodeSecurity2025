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
 * - Principle of least privilege: Only necessary DB permissions assumed.
 * - No secrets or credentials hardcoded.
 * - Output encoding for all user-facing messages.
 * - Assumes HTTPS is enforced at deployment level.
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    // Validate email format strictly
    if (
        typeof email !== "string" ||
        !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
    ) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid request.");
    }

    // Create a MySQL connection using environment variables (never hardcode secrets)
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        // Secure defaults
        port: process.env.DB_PORT || 3306,
        ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
    };

    const connection = mysql.createConnection(dbConfig);

    // Principle of least privilege: Only allow SELECT and UPDATE on the relevant table
    connection.connect((err) => {
        if (err) {
            // Log error internally, generic message to client
            console.error("DB connection error:", err);
            return res.status(500).send("Server error.");
        }

        // Use parameterized query to prevent SQL injection
        connection.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (selectErr, results) => {
                if (selectErr) {
                    console.error("DB select error:", selectErr);
                    connection.end();
                    return res.status(500).send("Server error.");
                }

                if (results.length === 0) {
                    // Do not reveal whether email exists (privacy)
                    connection.end();
                    return res.status(200).send("If your email is subscribed, it will be unsubscribed.");
                }

                // Unsubscribe: Use parameterized query
                connection.query(
                    "UPDATE subscribers SET subscribed = 0 WHERE email = ?",
                    [email],
                    (updateErr) => {
                        connection.end();
                        if (updateErr) {
                            console.error("DB update error:", updateErr);
                            return res.status(500).send("Server error.");
                        }
                        // Generic success message, no sensitive info
                        return res.status(200).send("If your email is subscribed, it will be unsubscribed.");
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
