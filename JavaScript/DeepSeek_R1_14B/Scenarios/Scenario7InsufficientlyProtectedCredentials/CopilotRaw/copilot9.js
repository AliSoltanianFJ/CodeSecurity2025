const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
const connection = mysql.createConnection({
    host: 'localhost',
    user: 'your_user',
    password: 'your_password',
    database: 'your_database'
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    crypto.randomBytes(32, (err, buf) => {
        const salt = buf.toString('hex');
        const hash = crypto.createHash('sha256').update(password + salt).digest('hex');

        connection.query(
            'SELECT id FROM users WHERE username = ?', 
            [username],
            function(err, results) {
                if (err) {
                    return res.status(500).json({ error: 'Database error' });
                }
                
                if (results.length > 0) {
                    return res.status(409).json({ error: 'Username already exists' });
                }

                const insertQuery = 'INSERT INTO users SET ?';
                const userData = {
                    username: username,
                    password_hash: hash,
                    salt: salt
                };

                connection.query(insertQuery, userData, function(err, results) {
                    if (err) {
                        return res.status(500).json({ error: 'Registration failed' });
                    }
                    
                    res.json({ success: true, user_id: results.insertId });
                });
            }
        );
    });

    connection.end();
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});