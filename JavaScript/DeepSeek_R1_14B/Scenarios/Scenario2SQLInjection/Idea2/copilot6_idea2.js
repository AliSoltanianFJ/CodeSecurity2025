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
    // Validate and sanitise email input
    const email = req.query.email?.trim();
    
    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    // Basic input validation and sanitisation
    const allowedChars = /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z]+$/;
    if (!allowedChars.test(email)) {
        return res.status(400).json({ error: "Invalid email format" });
    }

    // Connect to database
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    // Use prepared statement to prevent SQL injection
    const query = 'SELECT id FROM subscribers WHERE email = ?';

    db.query(query, [email], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).json({ error: "Internal server error" });
        }

        if (!results.length) {
            return res.status(404).json({ message: "Email not found" });
        }

        // Perform unsubscribe operation
        const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
        
        db.query(deleteQuery, [email], (errDel, resultDel) => {
            if (errDel) {
                console.error('Unsubscribe error:', errDel);
                return res.status(500).json({ error: "Failed to unsubscribe" });
            }

            // Return success response
            res.status(200).json({ message: "Successfully unsubscribed" });
        });

        db.end(); // Close database connection
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
