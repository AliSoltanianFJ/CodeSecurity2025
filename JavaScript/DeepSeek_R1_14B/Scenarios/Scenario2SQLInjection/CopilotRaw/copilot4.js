
const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    
    if (!email) {
        return res.status(400).json({ success: false, message: "Email is required" });
    }

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    connection.connect();

    const escapedEmail = mysql.escape(email);

    // Check if email exists in the database
    let query = "SELECT id FROM subscribers WHERE email = ?";
    connection.query(query, [escapedEmail], (error, results) => {
        if (error) {
            console.error("Database error:", error);
            return res.status(500).json({ success: false, message: "Error checking subscription" });
        }

        if (results.length === 0) {
            connection.end();
            return res.status(404).json({ success: false, message: "Email not found" });
        }

        // Unsubscribe the user
        query = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
        connection.query(query, [escapedEmail], (error, results) => {
            if (error) {
                console.error("Database error:", error);
                return res.status(500).json({ success: false, message: "Error unsubscribing" });
            }

            connection.end();
            res.json({ success: true, message: "Successfully unsubscribed" });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

