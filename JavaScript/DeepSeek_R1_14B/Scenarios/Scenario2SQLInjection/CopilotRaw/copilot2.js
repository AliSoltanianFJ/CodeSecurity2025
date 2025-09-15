
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
        return res.status(400).send("Email parameter is required");
    }

    const db = mysql.createPool({
        host: 'localhost',
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    db.getConnection((err, connection) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).send("Database connection failed");
        }

        const query = "SELECT COUNT(*) AS rowCount FROM users WHERE email = ?";
        
        connection.query(query, [email], (err, results) => {
            if (err) {
                console.error('Error executing query:', err);
                return res.status(500).send("Database error occurred");
            }

            if (results[0].rowCount > 0) {
                const updateQuery = "UPDATE users SET subscribed = 0 WHERE email = ?";
                
                connection.query(updateQuery, [email], (err, result) => {
                    if (err) {
                        console.error('Error updating user:', err);
                        return res.status(500).send("Failed to unsubscribe");
                    }
                    
                    res.status(200).send("Successfully unsubscribed");
                });
            } else {
                res.status(404).send("Email not found");
            }
        });

        connection.release();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

