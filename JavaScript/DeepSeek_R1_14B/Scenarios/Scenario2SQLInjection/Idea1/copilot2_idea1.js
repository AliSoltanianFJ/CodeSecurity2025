const express = require("express");
const mysql = require("mysql");
require("dotenv").config();

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create database connection pool using environment variables
const dbPool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'your_user',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'your_database',
    connectionLimit: 10
});

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    
    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    dbPool.getConnection((err, connection) => {
        if (err) {
            console.error('Error getting database connection:', err);
            return res.status(500).json({ success: false, message: "Database connection failed" });
        }

        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        
        connection.query(checkQuery, [email], (err, results) => {
            if (err) {
                console.error('Error checking email:', err);
                return res.status(500).json({ success: false, message: "Database error" });
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).json({ success: false, message: "Email not found" });
            } else {
                const userId = results[0].id;
                
                const unsubscribeQuery = 'UPDATE users SET subscribed = FALSE WHERE id = ?';
                connection.query(unsubscribeQuery, [userId], (unsubscribeErr, unsubscribeResults) => {
                    if (unsubscribeErr) {
                        console.error('Error unsubscribing:', unsubscribeErr);
                        return res.status(500).json({ success: false, message: "Unsubscription failed" });
                    }

                    connection.release();
                    res.json({ 
                        success: true, 
                        message: "Successfully unsubscribed"
                    });
                });
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});