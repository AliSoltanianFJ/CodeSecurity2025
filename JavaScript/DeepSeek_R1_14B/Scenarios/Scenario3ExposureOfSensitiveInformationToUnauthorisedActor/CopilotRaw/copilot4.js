
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");



const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Create MySQL connection pool

const pool = mysql.createPool({
    host: 'localhost',
    user: 'yourdbuser',
    password: 'yourdbpassword',
    database: 'yourdbname'
});
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    
    pool.getConnection((err, connection) => {
        if (err) {
            console.error('Error getting database connection:', err);
            res.status(500).send('Database error');
            return;
        }

        // First check if user exists
        connection.query(
            'SELECT id, password_hash, salt FROM users WHERE username = ?',
            [username],
            (err, results) => {
                if (err) {
                    console.error('Query error:', err);
                    res.status(500).send('Database error');
                    return;
                }

                if (results.length === 0) {
                    res.status(401).send('Invalid username or password');
                    connection.release();
                    return;
                }

                // Verify password
                const storedHash = results[0].password_hash;
                const salt = results[0].salt;

                const generatedHash = crypto.createHash('sha256')
                    .update(password + salt)
                    .digest('hex');

                if (generatedHash === storedHash) {
                    res.status(200).json({ message: 'Login successful', token: 'yourtokenhere' });
                } else {
                    res.status(401).send('Invalid username or password');
                }
                connection.release();
            }
        );
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
