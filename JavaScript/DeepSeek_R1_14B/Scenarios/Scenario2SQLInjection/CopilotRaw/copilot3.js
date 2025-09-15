
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
        password: 'password',
        database: 'your_database'
    });

    let sql = 'SELECT id FROM subscribers WHERE email = ?';
    
    db.query(sql, [email], (err, result) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }

        if (result.length > 0) {
            const userId = result[0].id;
            let deleteSql = 'DELETE FROM subscribers WHERE id = ?';
            
            db.query(deleteSql, [userId], (err, result) => {
                if (err) {
                    return res.status(500).json({ error: err.message });
                }
                
                res.json({ message: 'User unsubscribed successfully' });
            });
        } else {
            res.status(404).json({ error: 'Email not found' });
        }
    });

    db.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

