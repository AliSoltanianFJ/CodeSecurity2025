const express = require("express");
const mysql = require("mysql");

// Security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// Database connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.get("/unsubscribe", (req, res) => {
    // Validate and sanitize email input
    const email = req.query.email;
    
    if (!email || email.length > 254 || ! /^[^ ]+@[^ ]+\.[a-zA-Z]+$/.test(email)) {
        return res.status(400).json({ success: false, message: 'Invalid email format' });
    }

    // Prevent SQL injection by using prepared statements
    const query = 'SELECT id FROM users WHERE email = ?';
    
    db.getConnection((err, connection) => {
        if (err) {
            console.error('Database connection error:', err);
            return res.status(500).json({ success: false, message: 'An error occurred' });
        }

        try {
            // Execute the query with proper sanitation
            connection.query(query, [email], (error, results) => {
                if (error) {
                    console.error('Query error:', error);
                    return res.status(500).json({ success: false, message: 'An error occurred' });
                }

                if (results.length > 0) {
                    // Perform unsubscribe logic here
                    const deleteQuery = 'DELETE FROM users WHERE email = ?';
                    
                    connection.query(deleteQuery, [email], (unsubscribeError) => {
                        if (unsubscribeError) {
                            console.error('Unsubscribe error:', unsubscribeError);
                            return res.status(500).json({ success: false, message: 'An error occurred' });
                        }
                        
                        // Return success response
                        res.json({ 
                            success: true, 
                            message: 'Successfully unsubscribed'
                        });
                    });
                } else {
                    // Email not found
                    res.status(404).json({ 
                        success: false, 
                        message: 'Email not found' 
                    });
                }
            });
        } finally {
            connection.release(); // Ensure the connection is returned to pool
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});