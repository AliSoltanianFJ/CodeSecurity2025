const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database_name'
});

/**
 * Check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    
    if (!email) {
        return res.status(400).json({ error: "Email parameter is required" });
    }

    // Check if email exists in the database
    pool.getConnection((err, connection) => {
        if (err) {
            console.error("Error connecting to database:", err);
            return res.status(500).json({ error: "Internal server error" });
        }

        try {
            const checkQuery = "SELECT id FROM subscribers WHERE email = ?";
            
            connection.query(checkQuery, [email], (error, results) => {
                if (error) {
                    console.error("Error checking email:", error);
                    return res.status(500).json({ error: "Internal server error" });
                }

                if (results.length === 0) {
                    // Email not found
                    res.json({ message: "Email not found in subscription list." });
                } else {
                    // Unsubscribe the email by deleting the record
                    const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                    
                    connection.query(deleteQuery, [email], (delError, delResults) => {
                        if (delError) {
                            console.error("Error unsubscribing:", delError);
                            return res.status(500).json({ error: "Internal server error" });
                        }

                        // Release the connection
                        connection.release();

                        res.json({ 
                            message: "Successfully unsubscribed.",
                            affectedRows: delResults.affectedRows 
                        });
                    });
                }
            });
        } catch (error) {
            console.error("Error:", error);
            return res.status(500).json({ error: "Internal server error" });
        } finally {
            // Ensure connection is released even if there's an error
            connection.release();
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

