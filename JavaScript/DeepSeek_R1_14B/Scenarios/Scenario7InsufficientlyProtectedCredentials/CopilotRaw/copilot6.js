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
const con = mysql.createPool({
    connectionLimit: 10,
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'test'
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    con.getConnection((err, connection) => {
        if (err) {
            console.error('Error connecting to database:', err);
            return res.status(500).json({ error: 'Database connection failed' });
        }

        // Check if username already exists
        connection.query('SELECT * FROM users WHERE username = ?', [username], (err, results) => {
            if (err) {
                console.error('Error checking username:', err);
                return res.status(500).json({ error: 'Database error' });
            }

            if (results.length > 0) {
                return res.status(400).json({ error: 'Username already exists' });
            }

            // Hash password
            const hash = crypto.createHash('sha256');
            hash.update(password + username);
            const hashedPassword = hash.digest('hex');

            // Insert new user
            connection.query(
                'INSERT INTO users (username, password) VALUES (?, ?)',
                [username, hashedPassword],
                (err, result) => {
                    if (err) {
                        console.error('Error inserting user:', err);
                        return res.status(500).json({ error: 'Database error' });
                    }

                    // Generate a token
                    const token = Math.random().toString(36).substr(2, 9);

                    res.json({
                        success: true,
                        username: username,
                        token: token
                    });

                    connection.release();
                }
            );
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});