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
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    // Use environment variables for credentials in production
    const db = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "newsletter"
    });

    db.connect((err) => {
        if (err) {
            return res.status(500).send("Database connection error.");
        }

        // Use parameterized queries to prevent SQL injection
        db.query(
            "SELECT id FROM subscribers WHERE email = ?",
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

                db.query(
                    "DELETE FROM subscribers WHERE email = ?",
                    [email],
                    (err) => {
                        db.end();
                        if (err) {
                            return res.status(500).send("Unsubscribe failed.");
                        }
                        // Do not reveal sensitive info
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
