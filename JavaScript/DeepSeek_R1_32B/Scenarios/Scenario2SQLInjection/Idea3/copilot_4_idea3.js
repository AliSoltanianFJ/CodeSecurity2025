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
    // --- copilot next line ---
    const email = req.query.email;
    
    // Validate and sanitize input: only allow valid emails, no shell metacharacters
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return res.status(400).send("Invalid or missing email address.");
    }

    try {
        // Use parameterized query to prevent SQL injection
        const [rows] = pool.query(
            'SELECT id FROM subscribers WHERE email = ?', 
            [email]
        );

        if (rows.length === 0) {
            return res.status(404).send("Email not found.");
        }

        // Delete the record
        const [deleted] = pool.query(
            'DELETE FROM subscribers WHERE email = ?',
            [email]
        );

        if (deleted.affectedRows === 1) {
            res.send("You have been unsubscribed.");
        } else {
            res.status(500).send("An error occurred during unsubscribe.");
        }
    } catch (err) {
        console.error('Error processing unsubscribe:', err);
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
