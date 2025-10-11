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
    // Validate and sanitize input
    const email = req.query.email;
    
    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
        return res.status(400).json({ error: "Invalid email format" });
    }

    // Prevent SQL injection by using prepared statements
    const query = 'SELECT id FROM subscribers WHERE email = ?';
    
    try {
        const connection = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        connection.connect((err) => {
            if (err) {
                return res.status(500).json({ error: "Database connection failed" });
            }

            connection.query(query, [email], (error, results) => {
                if (error) {
                    console.error("Query error:", error);
                    return res.status(500).json({ error: "Internal server error" });
                }

                if (results.length === 0) {
                    return res.status(404).json({ message: "Email not found in database" });
                }

                // Unsubscribe by deleting the record
                const unsubscribeQuery = 'DELETE FROM subscribers WHERE email = ?';
                
                connection.query(unsubscribeQuery, [email], (delError, deleteResults) => {
                    if (delError) {
                        console.error("Unsubscribe error:", delError);
                        return res.status(500).json({ error: "Failed to unsubscribe" });
                    }

                    connection.end();

                    if (deleteResults.affectedRows > 0) {
                        res.json({ message: "Successfully unsubscribed" });
                    } else {
                        res.status(404).json({ message: "No subscription found to unsubscribe" });
                    }
                });
            });
        });
    } catch (error) {
        console.error("Unexpected error:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
