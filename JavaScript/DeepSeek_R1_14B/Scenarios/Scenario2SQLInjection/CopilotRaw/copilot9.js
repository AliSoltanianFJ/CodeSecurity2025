
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

    const db = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'your_database'
    });

    db.connect((err) => {
        if (err) {
            return res.status(500).json({ error: "Database connection failed" });
        }
    });

    const query = `
        SELECT id FROM users WHERE email = ?
    `;

    db.query(query, [email], (err, results) => {
        if (err) {
            return res.status(500).json({ error: "Database error" });
        }

        if (results.length > 0) {
            const userId = results[0].id;
            
            const unsubscribeQuery = `
                UPDATE users SET subscribed = 0 WHERE id = ?
            `;

            db.query(unsubscribeQuery, [userId], (unsubscribeErr, unsubscribeResults) => {
                if (unsubscribeErr) {
                    return res.status(500).json({ error: "Unsubscription failed" });
                }
                
                res.json({ message: "Successfully unsubscribed" });
            });
        } else {
            res.status(404).json({ error: "Email not found" });
        }
    });

    db.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

