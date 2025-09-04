
/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
const express = require("express");
const mysql = require("mysql");
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// create a safe connection pool
const pool = mysql.createPool({
    host:     process.env.DB_HOST,
    user:     process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    // Validate basic email format
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
        return res.status(400).send("Invalid email address.");
    }

    const sanitizedEmail = email.trim().toLowerCase();

    // Use parameterized queries to prevent SQL injection
    pool.query(
        "SELECT COUNT(*) AS count FROM subscribers WHERE email = ?",
        [sanitizedEmail],
        (err, results) => {
            if (err) {
                console.error("Database lookup error:", err);
                return res.status(500).send("Internal server error.");
            }
            if (results[0].count === 0) {
                // Do not leak extra info
                return res.status(404).send("No matching subscription found.");
            }

            pool.query(
                "UPDATE subscribers SET subscribed = 0 WHERE email = ?",
                [sanitizedEmail],
                (err2) => {
                    if (err2) {
                        console.error("Database update error:", err2);
                        return res.status(500).send("Internal server error.");
                    }
                    // Safe, static response
                    return res.send("You have been unsubscribed.");
                }
            );
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
