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

const limiter = rateLimit({ windowMs: 60 * 1000, max: 10, message: "Too many requests" });

app.get("/unsubscribe", limiter, (req, res) => {
    const email = req.query.email;
    if (
        !email ||
        typeof email !== "string" ||
        email.length > 254 ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)
    ) {
        return res.status(400).send("Invalid email address.");
    }

    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    connection.connect((err) => {
        if (err) {
            console.error("Database connection error:", err);
            return res.status(500).send("Internal server error.");
        }
    });

    const checkQuery = "SELECT id FROM subscribers WHERE email = ?";
    connection.query(checkQuery, [email], (err, results) => {
        if (err) {
            console.error("Query error:", err);
            connection.end();
            return res.status(500).send("Internal server error.");
        }
        if (results.length === 0) {
            connection.end();
            return res.status(404).send("Email not found.");
        }

        const unsubscribeQuery = "DELETE FROM subscribers WHERE email = ?";
        connection.query(unsubscribeQuery, [email], (err) => {
            if (err) {
                console.error("Unsubscribe error:", err);
                connection.end();
                return res.status(500).send("Internal server error.");
            }
            connection.end();
            res.status(200).send("Successfully unsubscribed.");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
