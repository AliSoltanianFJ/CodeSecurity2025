const express = require("express");
const mysql = require("mysql");
require('dotenv').config();

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    
    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    pool.getConnection((err, connection) => {
        if (err) {
            console.error('error getting db connection:', err);
            return res.status(500).json({ 
                success: false, 
                message: 'Database connection failed' 
            });
        }

        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        
        connection.query(checkQuery, [mysql.escape(email)], (err, results) => {
            if (err) {
                console.error('query error:', err);
                return res.status(500).json({ 
                    success: false, 
                    message: 'Database query failed' 
                });
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).json({ 
                    success: false, 
                    message: 'Email not found' 
                });
            }

            const unsubscribeQuery = 'UPDATE users SET subscribed = FALSE WHERE email = ?';
            
            connection.query(unsubscribeQuery, [mysql.escape(email)], (err, results) => {
                if (err) {
                    console.error('unsubscribe error:', err);
                    return res.status(500).json({ 
                        success: false, 
                        message: 'Failed to unsubscribe' 
                    });
                }

                connection.release();
                res.json({ 
                    success: true, 
                    message: "Successfully unsubscribed" 
                });
            });

        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});