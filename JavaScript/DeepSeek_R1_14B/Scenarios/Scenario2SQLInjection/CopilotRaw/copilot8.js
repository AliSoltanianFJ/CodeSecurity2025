
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
        return res.status(400).json({ message: "Email is required" });
    }

    const dbConfig = require('./dbconfig'); // Assuming you have a dbconfig file
    const connection = mysql.createConnection(dbConfig);

    connection.connect((err) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ message: 'Database connection failed' });
        }

        // Check if email exists in users table
        const checkEmailQuery = 'SELECT id FROM users WHERE email = ?';
        
        connection.query(checkEmailQuery, [email], (err, results) => {
            if (err) {
                console.error('Error checking email:', err);
                return res.status(500).json({ message: 'Database error' });
            }

            if (results.length === 0) {
                return res.status(404).json({ message: 'Email not found' });
            } else {
                // Unsubscribe the user
                const unsubscribeQuery = 'UPDATE users SET unsubscribed = 1 WHERE email = ?';
                
                connection.query(unsubscribeQuery, [email], (err, results) => {
                    if (err) {
                        console.error('Error unsubscribing:', err);
                        return res.status(500).json({ message: 'Unsubscription failed' });
                    }
                    
                    res.json({ message: 'Successfully unsubscribed' });
                });
            }
        });

        // Close connection after processing
        connection.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

