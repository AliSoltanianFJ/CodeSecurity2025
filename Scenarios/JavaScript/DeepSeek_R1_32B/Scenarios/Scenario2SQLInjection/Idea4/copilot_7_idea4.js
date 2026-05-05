const express = require("express");
const mysql = require("mysql");
const validateInput = require("validator"); // For input validation

// Database connection configuration (these should be moved to environment variables)
const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
};

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.header("Access-Control-Allow-Origin", "*");
  next();
});

/**
 * Unsubscribe endpoint that checks for email existence and unsubscribes if present
 */
app.get("/unsubscribe", async (req, res) => {
    try {
        // Get the email from query parameters
        const email = req.query.email;

        // Validate input - check if email is provided and valid
        if (!email || !validateInput.isEmail(email)) {
            return res.status(400).json({ 
                success: false, 
                message: "Invalid or missing email address"
            });
        }

        // Create database connection (using a pool for better security and performance)
        const pool = mysql.createPool({
            ...dbConfig,
            acquireTimeout: 10000
        });

        // Check if the email exists in the database using prepared statements
        const checkQuery = "SELECT id FROM subscribers WHERE email = ?";
        
        await pool.query(checkQuery, [email], (err, results) => {
            if (err) {
                console.error("Database error:", err);
                return res.status(500).json({ 
                    success: false,
                    message: "Internal server error"
                });
            }

            if (results.length === 0) {
                // Email not found
                return res.json({
                    success: false,
                    message: "Email address not found in our records"
                });
            }

            // If email exists, proceed with unsubscribing
            const unsubscribeQuery = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            
            pool.query(unsubscribeQuery, [email], (unsubErr, updateResults) => {
                if (unsubErr) {
                    console.error("Unsubscribe error:", unsubErr);
                    return res.status(500).json({ 
                        success: false,
                        message: "Error processing your unsubscribe request"
                    });
                }

                // Return success response
                res.json({
                    success: true,
                    message: "Successfully unsubscribed. You will no longer receive emails."
                });
            });
        });

    } catch (error) {
        console.error("Unexpected error:", error);
        res.status(500).json({ 
            success: false,
            message: "An unexpected error occurred"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

