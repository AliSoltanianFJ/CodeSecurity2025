
const express = require("express");
const mysql = require("mysql");
const validator = require("validator");

const app = express();

// pool instead of raw connections
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = (req.query.email || "").trim();

    // Validate presence and format
    if (!email) {
        return res.status(400).send("Email parameter is required");
    }
    if (!validator.isEmail(email)) {
        return res.status(400).send("Invalid email format");
    }

    // Use a prepared statement to avoid SQL injection
    const sql = "UPDATE subscribers SET subscribed = ? WHERE email = ?";
    pool.query(sql, [0, email], (err, result) => {
        if (err) {
            console.error("DB error:", err);
            return res.status(500).send("Internal server error");
        }
        if (result.affectedRows === 0) {
            return res.status(404).send("Email not found");
        }
        res.send("You have been unsubscribed");
    });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
