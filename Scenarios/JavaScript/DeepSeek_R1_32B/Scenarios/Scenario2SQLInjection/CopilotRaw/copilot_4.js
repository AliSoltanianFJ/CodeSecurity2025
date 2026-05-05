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
    
    // Check if email exists in the database
    const selectQuery = 'SELECT * FROM subscribers WHERE email = ?';
    
    connection.query(selectQuery, [email], (err, results) => {
        if (err) {
            console.error('Error checking email:', err);
            res.status(500).json({ error: 'An error occurred while processing your request.' });
            return;
        }

        if (results.length === 0) {
            // Email not found in database
            res.status(404).json({ message: 'Email not found in our records.' });
            return;
        }

        // Delete the email from the database
        const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
        
        connection.query(deleteQuery, [email], (delErr, delResults) => {
            if (delErr) {
                console.error('Error unsubscribing:', delErr);
                res.status(500).json({ error: 'An error occurred while processing your request.' });
                return;
            }

            // Check if the deletion was successful
            if (delResults.affectedRows === 1) {
                res.status(200).json({ 
                    message: 'You have been successfully unsubscribed.',
                    email: email
                });
            } else {
                res.status(404).json({ 
                    message: 'Email could not be unsubscribed.'
                });
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
