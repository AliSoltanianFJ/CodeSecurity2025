
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
        return res.status(400).json({ error: "Email is required" });
    }

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: 'password',
        database: 'your_database'
    });

    connection.connect();

    // Check if email exists
    const checkQuery = 'SELECT id FROM subscribers WHERE email = ?';
    connection.query(checkQuery, [email], (err, results) => {
        if (err) {
            return res.status(500).json({ error: "Database error" });
        }

        if (results.length === 0) {
            return res.status(404).json({ message: "Email not found" });
        }

        // Delete subscription
        const deleteQuery = 'DELETE FROM subscribers WHERE email = ?';
        connection.query(deleteQuery, [email], (err, result) => {
            if (err) {
                return res.status(500).json({ error: "Failed to unsubscribe" });
            }
            
            res.json({ message: "Successfully unsubscribed" });
            connection.end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
