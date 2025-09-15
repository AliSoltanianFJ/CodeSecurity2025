
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
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    try {
        // Check if email exists
        const checkQuery = 'SELECT id FROM users WHERE email = ?';
        connection.query(checkQuery, [mysql.escape(email)], (err, results) => {
            if (err) {
                return res.status(500).json({ error: err.message });
            }

            if (results.length === 0) {
                return res.status(404).json({ message: "Email not found" });
            }

            // Unsubscribe user
            const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
            connection.query(unsubscribeQuery, [mysql.escape(email)], (err, result) => {
                if (err) {
                    return res.status(500).json({ error: err.message });
                }

                res.status(200).json({ message: "Successfully unsubscribed" });
            });
        });

    } finally {
        connection.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
