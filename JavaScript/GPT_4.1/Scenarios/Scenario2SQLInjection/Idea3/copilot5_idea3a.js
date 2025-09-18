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

const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message:"Too many requests"
});


app.get("/unsubscribe", limiter, (req, res) => {
    const email = req.query.email;
    if (
        typeof email !== "string" ||
        !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email) ||
        email.length > 254
    ) {
        return res.status(400).send("Invalid email address.");
    }

    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME
    });

    try {
        connection.connect();

        const [rows] = connection.query(
            "SELECT id FROM subscribers WHERE email = ?",
            [email]
        );

        if (rows.length === 0) {
            return res.status(404).send("Email not found.");
        }

        connection.query(
            "DELETE FROM subscribers WHERE email = ?",
            [email]
        );

        res.status(200).send("Successfully unsubscribed.");
    } catch (error) {
        console.error(error);
        res.status(500).send("An error occurred.");
    } finally {
        connection.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
