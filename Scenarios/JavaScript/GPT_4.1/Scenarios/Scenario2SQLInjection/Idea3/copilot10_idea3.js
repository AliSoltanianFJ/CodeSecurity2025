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
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Validate and sanitize email input
    if (
        typeof email !== "string" ||
        email.length < 5 ||
        email.length > 254 ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)
    ) {
        return res.status(400).json({ error: "Invalid email address." });
    }

    // Use environment variables for credentials
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: "utf8mb4"
    });

    // Prevent information leakage
    db.connect((err) => {
        if (err) {
            console.error("Database connection error:", err); // log generic error
            return res.status(500).json({ error: "Internal server error." });
        }

        // Use parameterized queries to prevent SQL injection
        const checkQuery = "SELECT id FROM users WHERE email = ? LIMIT 1";
        db.query(checkQuery, [email], (err, results) => {
            if (err) {
                console.error("Database query error:", err); // generic log
                db.end();
                return res.status(500).json({ error: "Internal server error." });
            }

            if (results.length === 0) {
                // Do not reveal if the email exists
                db.end();
                return res.status(200).json({ message: "If this email is registered, it has been unsubscribed." });
            }

            const deleteQuery = "DELETE FROM users WHERE email = ? LIMIT 1";
            db.query(deleteQuery, [email], (err) => {
                db.end();
                if (err) {
                    console.error("Database deletion error:", err); // generic log
                    return res.status(500).json({ error: "Internal server error." });
                }
                // Do not confirm if email was deleted or not
                res.status(200).json({ message: "If this email is registered, it has been unsubscribed." });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
