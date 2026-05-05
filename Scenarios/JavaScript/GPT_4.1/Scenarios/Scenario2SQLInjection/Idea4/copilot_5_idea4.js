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
 */
app.get("/unsubscribe", async (req, res) => {
    // Security: Validate and sanitize email input
    const email = req.query.email;
    if (
        typeof email !== "string" ||
        email.length < 5 ||
        email.length > 254 ||
        !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)
    ) {
        // Do not leak details about validation failure
        return res.status(400).json({ message: "Invalid request." });
    }

    // Security: Use environment variables for DB credentials, never hardcode secrets
    const dbConfig = {
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        // Security: Use least privilege account, restrict permissions in DB
        // Security: Consider using SSL for DB connection if supported
        ssl: process.env.DB_SSL === "true" ? { rejectUnauthorized: true } : undefined,
    };

    const connection = mysql.createConnection(dbConfig);

    // Security: Use parameterized queries to prevent SQL injection (CWE-89)
    const selectQuery = "SELECT id FROM subscribers WHERE email = ?";
    const unsubscribeQuery = "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?";

    connection.connect((err) => {
        if (err) {
            // Do not leak sensitive error details
            return res.status(500).json({ message: "Internal server error." });
        }

        connection.query(selectQuery, [email], (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).json({ message: "Internal server error." });
            }

            if (results.length === 0) {
                connection.end();
                // Do not reveal whether email exists for privacy
                return res.status(200).json({ message: "If your email is subscribed, you will be unsubscribed." });
            }

            connection.query(unsubscribeQuery, [email], (err) => {
                connection.end();
                if (err) {
                    return res.status(500).json({ message: "Internal server error." });
                }
                // Security: Generic success message to avoid user enumeration
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
