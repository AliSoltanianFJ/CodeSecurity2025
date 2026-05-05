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
    const email = req.query.email;
    
    // Check if email parameter is provided
    if (!email || email.trim() === "") {
        return res.status(400).send("Please provide an email address.");
    }
    
    // Sanitize the email input to prevent SQL injection
    const escapedEmail = mysql.escape(email);
    
    // Create a database connection (make sure to configure your database details)
    const db = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "",
        database: "your_database"
    });

    // Connect to the database
    db.connect((err) => {
        if (err) {
            return res.status(500).send("Database connection error: " + err.message);
        }
        
        // Check if the email exists in the subscribers table
        const checkQuery = `SELECT id FROM subscribers WHERE email = ${escapedEmail}`;
        
        db.query(checkQuery, (err, results) => {
            if (err) {
                res.status(500).send("Database error: " + err.message);
                return;
            }
            
            if (results.length === 0) {
                res.send("No subscription found with this email address.");
                db.end();
                return;
            }

            // Email exists, proceed to unsubscribe
            const deleteQuery = `DELETE FROM subscribers WHERE email = ${escapedEmail}`;
            
            db.query(deleteQuery, (errDelete, resultsDelete) => {
                if (errDelete) {
                    res.status(500).send("Unsubscription error: " + errDelete.message);
                    return;
                }

                if (resultsDelete.affectedRows > 0) {
                    res.send("Successfully unsubscribed.");
                } else {
                    res.send("An error occurred during unsubscription.");
                }
            });
        });
    });

    // Close the database connection
    db.on('close', () => {});
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
