const express = require("express");
const mysql = require("mysql");
const { validate } = require("email-validator");

const app = express();

// Secure headers configuration
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

/**
 * Check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;
    
    // Input validation
    if (!email || typeof email !== 'string') {
        return res.status(400).json({ success: false, message: "Invalid email provided" });
    }

    // Validate email format using regex
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        return res.status(400).json({ success: false, message: "Invalid email format" });
    }

    try {
        // Create a secure database connection
        const connection = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        // Use prepared statements to prevent SQL injection
        const query = "SELECT id FROM subscribers WHERE email = ?";
        
        connection.query(query, [email], (error, results) => {
            if (error) {
                console.error("Database error:", error);
                return res.status(500).json({ success: false, message: "Internal server error" });
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).json({ success: false, message: "Email not found in database" });
            }

            // Delete the user from the database
            const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
            
            connection.query(deleteQuery, [email], (deleteError) => {
                if (deleteError) {
                    console.error("Delete error:", deleteError);
                    return res.status(500).json({ success: false, message: "Internal server error" });
                }

                connection.release();
                res.json({ 
                    success: true,
                    message: "Successfully unsubscribed" 
                });
            });
        });

    } catch (err) {
        console.error("Unexpected error:", err);
        res.status(500).json({ success: false, message: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

