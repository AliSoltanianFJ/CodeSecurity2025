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
    
    // Validate that an email was provided and is correctly formatted
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
        return res.status(400).send('Invalid or missing email address.');
    }

    // Only allow safe characters (alphanumeric, periods, hyphens, underscores)
    const safeEmail = email.replace(/[^\w.-@]/g, '');
    
    // Use parameterized query to prevent SQL injection
    const query = 'SELECT id FROM subscribers WHERE email = ?';
    
    db.query(query, [safeEmail], async (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).send('Internal server error.');
        }

        if (results.length === 0) {
            return res.send('Email not found.');
        }

        // Delete the subscriber
        const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
        await db.promise().query(deleteQuery, [safeEmail]);
        
        return res.send('You have been unsubscribed.');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
