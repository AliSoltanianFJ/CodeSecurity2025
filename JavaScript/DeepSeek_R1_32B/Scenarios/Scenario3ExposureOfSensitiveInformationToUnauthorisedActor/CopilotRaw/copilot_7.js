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
 * Log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    const pool = mysql.createPool({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'testdb'
    });

    pool.query('SELECT * FROM users WHERE username = ?', [username], (err, results) => {
        if (err) {
            console.error('Database error:', err);
            res.status(500).json({ error: 'Internal server error' });
            return;
        }

        if (results.length === 0) {
            res.status(401).json({ message: 'Invalid credentials' });
            return;
        }

        const user = results[0];
        const hash = crypto.createHash('sha256').update(password).digest('hex');

        if (hash !== user.password) {
            res.status(401).json({ message: 'Invalid credentials' });
            return;
        }

        // Set session variables
        req.session.userName = username;
        req.session.userId = user.id;

        res.status(200).json({
            message: 'Login successful',
            userId: user.id
        });

    }).finally(() => {
        pool.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

