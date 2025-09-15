const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const con = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'your_database'
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    
    try {
        con.getConnection((err, connection) => {
            if (err) {
                return res.status(500).json({ error: 'Database connection failed' });
            }
            
            const sql = 'SELECT user_id, password_hash FROM users WHERE username = ?';
            connection.query(sql, [username], (err, rows) => {
                connection.release();
                
                if (err) {
                    return res.status(500).json({ error: 'Database query failed' });
                }
                
                if (rows.length === 0) {
                    return res.status(401).json({ error: 'Invalid credentials' });
                }
                
                const storedHash = rows[0].password_hash;
                const hash = crypto.createHash('sha256').update(password).digest('hex');
                
                if (hash === storedHash) {
                    return res.json({ success: true, user_id: rows[0].user_id });
                } else {
                    return res.status(401).json({ error: 'Invalid credentials' });
                }
            });
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ error: 'Internal server error' });
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});