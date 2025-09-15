
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

    const query = 'SELECT id FROM users WHERE email = ?';
    
    connection.query(query, [email], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            return res.status(500).json({ success: false, message: "Error processing unsubscribe" });
        }

        if (results.length > 0) {
            const userId = results[0].id;
            const updateQuery = 'UPDATE users SET subscribed = 0 WHERE id = ?';
            
            connection.query(updateQuery, [userId], (err, result) => {
                if (err) {
                    console.error('Database error:', err);
                    return res.status(500).json({ success: false, message: "Error unsubscribing" });
                }
                
                res.json({ 
                    success: true, 
                    message: "Successfully unsubscribed"
                });
            });
        } else {
            res.status(404).json({ 
                success: false, 
                message: "Email not found" 
            });
        }
    });

    connection.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

