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
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Input validation: check for valid email format
    if (
        typeof email !== "string" ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    // Use environment variables for DB credentials (never hardcode)
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: "utf8mb4"
    });

    db.connect((err) => {
        if (err) {
            // Don't leak sensitive info
            return res.status(500).send("Database connection error.");
        }

        // Use parameterized queries to prevent SQL injection
        db.query(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email],
            (err, results) => {
                if (err) {
                    db.end();
                    return res.status(500).send("Database error.");
                }
                if (results.length === 0) {
                    db.end();
                    return res.status(404).send("Email not found.");
                }

                // Unsubscribe (delete)
                db.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (err) => {
                        db.end();
                        if (err) {
                            return res.status(500).send("Unsubscribe failed.");
                        }
                        // Generic message, no info leak
                        res.send("You have been unsubscribed if your email was registered.");
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
