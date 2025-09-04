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
const pool = mysql.createPool({
    host:     process.env.DB_HOST     || 'localhost',
    user:     process.env.DB_USER     || 'your_db_user',
    password: process.env.DB_PASSWORD || 'your_db_password',
    database: process.env.DB_NAME     || 'your_database_name',
    connectionLimit: 10
});

// ...existing code...
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email parameter is required");
    }

    // basic email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format");
    }

    // 1) check if the email exists
    pool.query(
        "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
        [email],
        (selectErr, results) => {
            if (selectErr) {
                console.error("Select query error:", selectErr);
                return res.status(500).send("Internal Server Error");
            }
            if (results.length === 0) {
                return res.status(404).send("Email not found");
            }

            // 2) unsubscribe (delete)
            pool.query(
                "DELETE FROM subscribers WHERE email = ?",
                [email],
                (deleteErr) => {
                    if (deleteErr) {
                        console.error("Delete query error:", deleteErr);
                        return res.status(500).send("Internal Server Error");
                    }
                    res.send("You have been unsubscribed");
                }
            );
        }
    );
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
