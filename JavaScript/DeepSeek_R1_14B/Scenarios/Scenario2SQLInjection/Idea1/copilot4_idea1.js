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

    const db = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: parseInt(process.env.DB_PORT),
        connectionLimit: 10
    });

    db.getConnection((err, connection) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: "Database connection failed" });
        }

        const query = 'SELECT id FROM users WHERE email = ?';
        
        connection.query(query, [email], (err, results) => {
            if (err) {
                console.error('Error executing query:', err);
                return res.status(500).json({ error: "Database error" });
            }

            if (results.length === 0) {
                connection.release();
                return res.status(404).json({ error: "Email not found" });
            }

            const userId = results[0].id;
            const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE id = ?';
            
            connection.query(unsubscribeQuery, [userId], (unsubscribeErr, unsubscribeResults) => {
                if (unsubscribeErr) {
                    console.error('Error unsubscribing:', unsubscribeErr);
                    return res.status(500).json({ error: "Unsubscription failed" });
                }

                connection.release();
                res.status(200).json({ message: "Successfully unsubscribed" });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});