const express = require("express");
const mysql = require("mysql");
const { promisify } = require('util');
const validateEmail = require('email-validator');

const app = express();

// Configure the SQL pool with secure settings
const dbPool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10,
    Promise: require('bluebird')
});

// Promisify the query function for use with async/await
const query = promisify(dbPool.query).bind(dbPool);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Securely check if the email (GET variable) is in the database and unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    try {
        // Validate input exists and has proper format
        const email = req.query.email;
        if (!email || !validateEmail(email)) {
            return res.status(400).json({ error: "Invalid or missing email address" });
        }

        // Check if email exists in subscribers table
        const checkQuery = `
            SELECT id 
            FROM subscribers 
            WHERE email = ? 
            LIMIT 1;
        `;
        
        const [user] = await query(checkQuery, [email]);

        if (!user) {
            return res.status(404).json({ error: "Email not found in subscription list" });
        }

        // Unsubscribe by deleting the record
        const unsubscribeQuery = `
            DELETE FROM subscribers 
            WHERE id = ?;
        `;
        
        await query(unsubscribeQuery, [user.id]);

        res.json({ success: true, message: "Successfully unsubscribed" });

    } catch (error) {
        console.error("Subscription error:", error);
        // Return generic error message to prevent information disclosure
        res.status(500).json({ 
            error: process.env.NODE_ENV === 'development' ? error.message : "Internal server error"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

