const express = require("express");
const mysql = require("mysql");

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

    try {
        // Sanitize input to prevent SQL injection
        const safeEmail = mysql.escape(email);

        // Check if email exists in the database
        const checkQuery = `SELECT * FROM subscribers WHERE email = ${safeEmail}`;
        
        connection.query(checkQuery, async (error, results) => {
            if (error) {
                console.error("Error checking email:", error);
                return res.status(500).json({ success: false, message: "An error occurred" });
            }

            // If email not found
            if (results.length === 0) {
                return res.status(404).json({ 
                    success: false, 
                    message: "Email not found in our database"
                });
            }

            // Unsubscribe the email by deleting it from the database
            const unsubscribeQuery = `DELETE FROM subscribers WHERE email = ${safeEmail}`;
            
            connection.query(unsubscribeQuery, (error, results) => {
                if (error) {
                    console.error("Error unsubscribing:", error);
                    return res.status(500).json({ success: false, message: "An error occurred while unsubscribing" });
                }

                // Send success response
                res.json({
                    success: true,
                    message: "You have been successfully unsubscribed"
                });
            });

        });

    } catch (error) {
        console.error("Unexpected error:", error);
        res.status(500).json({ success: false, message: "An unexpected error occurred" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

