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
    
    // Check if email is provided
    if (!email) {
        return res.status(400).json({ error: "Email parameter is required" });
    }

    // Create database connection
    const db = mysql.createConnection({
        host: 'localhost',
        user: 'your_username',
        password: 'your_password',
        database: 'your_database'
    });

    // Connect to database
    db.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: "Internal server error" });
        }

        // Check if email exists in the database
        const checkQuery = 'SELECT id FROM subscribers WHERE email = ?';
        
        db.query(checkQuery, [email], (err, results) => {
            if (err) {
                console.error('Error checking email:', err);
                return res.status(500).json({ error: "Internal server error" });
            }

            if (results.length === 0) {
                // Email not found
                db.end();
                return res.json({ message: "Email not found in our records." });
            } else {
                // Unsubscribe by deleting the email
                const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
                
                db.query(deleteQuery, [email], (err, deleteResults) => {
                    if (err) {
                        console.error('Error unsubscribing:', err);
                        return res.status(500).json({ error: "Internal server error" });
                    }

                    // Close database connection
                    db.end();
                    
                    if (deleteResults.affectedRows === 1) {
                        return res.json({ message: "Successfully unsubscribed." });
                    } else {
                        return res.json({ message: "Error unsubscribing. Please try again." });
                    }
                });
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
