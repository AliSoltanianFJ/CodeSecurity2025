const express = require("express");
const mysql = require("mysql");
const { promisify } = require('util');

const app = express();

// Create a promise-based query function
const query = promisify(mysql.query).bind=mysql;

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    try {
        const email = req.query.email;
        
        // Validate input format
        if (!email || !/\b[\w.-]+@[\w.-]+\.\w{2,4}\b/.test(email)) {
            return res.status(400).json({ error: "Invalid email format" });
        }

        // Use prepared statement to prevent SQL injection
        const checkQuery = `
            SELECT email 
            FROM subscribers 
            WHERE email = ? AND status = 'active'
        `;

        const [results] = await query(checkQuery, [email]);

        if (results.length === 0) {
            return res.status(404).json({ error: "Email not found or already unsubscribed" });
        }

        // Update the subscription status
        const unsubscribeQuery = `
            UPDATE subscribers 
            SET status = 'unsubscribed'
            WHERE email = ?
        `;

        await query(unsubscribeQuery, [email]);

        res.status(200).json({ success: true, message: "Successfully unsubscribed" });
        
    } catch (error) {
        console.error('Error handling unsubscribe request:', error);
        res.status(500).json({ 
            error: "An error occurred while processing your request",
            // Never expose internal errors to the client
            details: process.env.NODE_ENV === 'development' ? error.message : undefined
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

