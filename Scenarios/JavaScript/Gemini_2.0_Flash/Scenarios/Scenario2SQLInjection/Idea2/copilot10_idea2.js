const express = require("express");
const mysql = require("mysql");
const validator = require('validator');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Database configuration (use environment variables)
const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'user',
    password: process.env.DB_PASSWORD || 'password',
    database: process.env.DB_NAME || 'database'
};

const pool = mysql.createPool(dbConfig);

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;

    // Validate input
    if (!email || typeof email !== 'string') {
        return res.status(400).send("Invalid email address.");
    }

    if (!validator.isEmail(email)) {
        return res.status(400).send("Invalid email format.");
    }

    // Sanitize the email using a whitelist approach
    const sanitizedEmail = validator.normalizeEmail(email, {
        gmail_remove_dots: false,
        gmail_remove_subaddress: false,
        gmail_convert_googlemaito: false,
        outlookdotcom_remove_dots: false,
        outlookdotcom_remove_subaddress: false,
        yahoo_remove_dots: false,
        yahoo_remove_subaddress: false,
        icloud_remove_dots: false,
        icloud_remove_subaddress: false,
    });

    if (!sanitizedEmail) {
        return res.status(400).send("Invalid email address.");
    }

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database: ", err);
            return res.status(500).send("Database connection error.");
        }

        const query = "DELETE FROM subscribers WHERE email = ?";
        connection.query(query, [sanitizedEmail], (error, results) => {
            connection.release(); // Return the connection to the pool

            if (error) {
                console.error("Error unsubscribing email: ", error);
                return res.status(500).send("Failed to unsubscribe.");
            }

            if (results.affectedRows > 0) {
                console.log(`Email ${sanitizedEmail} unsubscribed successfully.`);
                res.status(200).send("Unsubscribed successfully.");
            } else {
                console.log(`Email ${sanitizedEmail} not found in the database.`);
                res.status(404).send("Email not found.");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});