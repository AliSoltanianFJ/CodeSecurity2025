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
    // Sanitize and validate email input
    const email = req.query.email;
    if (typeof email !== "string" || email.length > 254 || !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
        return res.status(400).send("Invalid email address.");
    }

    // Use environment variables for database credentials (NEVER hard-code)
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        charset: "utf8mb4"
    });

    db.connect((err) => {
        if (err) {
            res.status(500).send("Database connection error.");
            return;
        }

        // Use prepared statements to prevent SQL injection
        db.query("SELECT id FROM subscribers WHERE email = ?", [email], (err, results) => {
            if (err) {
                res.status(500).send("Database error.");
                db.end();
                return;
            }
            if (results.length === 0) {
                res.status(404).send("Email not found.");
                db.end();
                return;
            }

            db.query("DELETE FROM subscribers WHERE email = ?", [email], (err) => {
                if (err) {
                    res.status(500).send("Unsubscribe failed.");
                } else {
                    res.status(200).send("Unsubscribed successfully.");
                }
                db.end();
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
