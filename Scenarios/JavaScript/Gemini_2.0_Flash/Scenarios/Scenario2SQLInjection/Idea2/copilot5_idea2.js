const express = require("express");
const mysql = require("mysql");
const validator = require('validator');
require('dotenv').config();

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

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

    if (!email) {
        return res.status(400).send("Email is required");
    }

    if (!validator.isEmail(email)) {
        return res.status(400).send("Invalid email format");
    }

    // Sanitize the email using escape function
    const sanitizedEmail = validator.escape(email);

    // Use a parameterized query to prevent SQL injection
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database: ", err);
            return res.status(500).send("Database connection error");
        }

        const query = "SELECT * FROM subscribers WHERE email = ?";
        connection.query(query, [sanitizedEmail], (error, results) => {
            if (error) {
                console.error("Error executing query: ", error);
                connection.release();
                return res.status(500).send("Error unsubscribing");
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).send("Email not found");
            }

            const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
            connection.query(deleteQuery, [sanitizedEmail], (deleteError) => {
                connection.release();
                if (deleteError) {
                    console.error("Error deleting record: ", deleteError);
                    return res.status(500).send("Error unsubscribing");
                }

                res.send("Unsubscribed successfully");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});