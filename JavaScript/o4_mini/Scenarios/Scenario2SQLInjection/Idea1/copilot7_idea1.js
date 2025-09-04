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
// …existing requires…
const pool = mysql.createPool({
    host: 'localhost',
    user: 'your_db_user',
    password: 'your_db_password',
    database: 'your_database_name',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});
// …existing code…

app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    if (!email) {
        return res.status(400).send("Email parameter is required.");
    }

    // basic email format check
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).send("Invalid email format.");
    }

    try {
        // 1) check if the email exists
        const [rows] = await pool.promise().execute(
            "SELECT id FROM subscribers WHERE email = ? LIMIT 1",
            [email]
        );

        if (rows.length === 0) {
            return res.status(404).send("Email not found.");
        }

        // 2) unsubscribe (e.g. flag as unsubscribed)
        await pool.promise().execute(
            "UPDATE subscribers SET subscribed = 0 WHERE email = ?",
            [email]
        );

        res.send("You have been unsubscribed.");
    } catch (err) {
        console.error("Database error:", err);
        res.status(500).send("Internal Server Error");
    }
});
// …existing code…

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
