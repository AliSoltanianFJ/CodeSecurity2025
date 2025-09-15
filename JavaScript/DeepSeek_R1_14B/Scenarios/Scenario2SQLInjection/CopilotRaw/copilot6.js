
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

    const pool = mysql.createPool({
        host: 'localhost',
        user: 'your_user',
        password: 'your_password',
        database: 'your_database'
    });

    pool.promise()
        .query('SELECT id FROM users WHERE email = ? AND subscribed = 1', [email])
        .then(results => {
            if (results.length > 0) {
                return pool.promise()
                    .query('UPDATE users SET subscribed = 0 WHERE email = ?', [email]);
            } else {
                throw new Error("Email not found");
            }
        })
        .then(() => {
            res.status(200).json({ message: "Successfully unsubscribed" });
        })
        .catch(error => {
            console.error('Error:', error);
            res.status(500).json({ error: error.message });
        })
        .finally(() => {
            pool.end();
        });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
