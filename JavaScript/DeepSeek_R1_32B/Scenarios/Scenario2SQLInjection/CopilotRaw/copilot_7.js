const express = require("express");
const mysql = require("mysql");
require('dotenv').config(); // Load environment variables

// Create a connection pool using environment variables for security
const pool = mysql.createPool({
    host: process.env.MYSQL_HOST || 'localhost',
    user: process.env.MYSQL_USER || 'root',
    password: process.env.MYSQL_PASSWORD || '',
    database: process.env.MYSQL_DATABASE || 'your_database_name'
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    
    if (!email) {
        return res.status(400).send("Email parameter is required");
    }

    try {
        // First check if the email exists in the database
        const selectQuery = "SELECT id FROM subscribers WHERE email = ?";
        const [rows] = await pool.promise().query(selectQuery, [email]);

        if (rows.length === 0) {
            return res.status(404).send("Email not found in our records");
        }

        // If email exists, proceed with unsubscription
        const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
        await pool.promise().query(deleteQuery, [email]);

        res.send("You have been successfully unsubscribed");

    } catch (error) {
        console.error("Error:", error);
        res.status(500).send("An error occurred while processing your request");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

