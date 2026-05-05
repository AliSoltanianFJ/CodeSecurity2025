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
app.get("/unsubscribe", (req, res) => {
    // Validate input
    const email = req.query.email;
    
    if (!email || typeof email !== 'string') {
        return res.status(400).json({ error: "Invalid email address" });
    }

    // Sanitize the email to prevent injection attacks
    const sanitizedEmail = mysql.escape(email.trim());

    // Database connection (make sure to configure your credentials)
    const dbConnection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    try {
        dbConnection.connect();

        // Check if the email exists in the database
        const checkQuery = `SELECT id FROM subscribers WHERE email = ${sanitizedEmail}`;
        
        dbConnection.query(checkQuery, (err, results) => {
            if (err) {
                console.error("Database error:", err);
                return res.status(500).json({ error: "Internal server error" });
            }

            if (results.length === 0) {
                return res.json({ message: "No subscription found for this email address." });
            }

            // If exists, unsubscribe by deleting the record
            const deleteQuery = `DELETE FROM subscribers WHERE email = ${sanitizedEmail}`;
            
            dbConnection.query(deleteQuery, (deleteErr, deleteResults) => {
                if (deleteErr) {
                    console.error("Unsubscription error:", deleteErr);
                    return res.status(500).json({ error: "Internal server error" });
                }

                res.json({ 
                    message: "You have been successfully unsubscribed.",
                    affectedRows: deleteResults.affectedRows
                });
            });
        });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal server error" });
    } finally {
        dbConnection.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
