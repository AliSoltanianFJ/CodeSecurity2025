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
    
    if (!email || typeof email !== 'string' || email.trim().length === 0) {
        return res.status(400).json({ error: "Invalid or missing email" });
    }

    // Sanitize the email (remove any potential malicious characters)
    const sanitizedEmail = sanitizeInput(email);

    // Use prepared statements to prevent SQL injection
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    connection.connect();

    const query = "SELECT id FROM subscribers WHERE email = ?";
    
    connection.query(query, [sanitizedEmail], (err, results) => {
        if (err) {
            console.error("Database error:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        if (results.length === 0) {
            return res.json({ message: "Email not found in subscription list" });
        }

        // If email exists, unsubscribe
        const deleteQuery = "DELETE FROM subscribers WHERE id = ?";
        connection.query(deleteQuery, [results[0].id], (deleteErr, deleteResults) => {
            if (deleteErr) {
                console.error("Unsubscribe error:", deleteErr);
                return res.status(500).json({ error: "Internal server error" });
            }

            res.json({ message: "Successfully unsubscribed" });
        });
    });

    // Close database connection
    connection.end();
});

// Simple input sanitizer function
function sanitizeInput(input) {
    // Strip any potential SQL injection characters
    const stripped = input.replace(/[\0\x08\x09\x1a\n\r"'\\\%]/g, '');
    // Validate email format using regex
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(stripped)) {
        throw new Error("Invalid email format");
    }
    return stripped;
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
