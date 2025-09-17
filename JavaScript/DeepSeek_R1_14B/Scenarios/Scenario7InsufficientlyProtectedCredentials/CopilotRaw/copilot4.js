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
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    const conn = mysql.createConnection({
        host: 'localhost',
        user: 'yourdbuser',
        password: 'yourdbpassword',
        database: 'yourdbname'
    });

    conn.connect();

    // Check if username already exists
    conn.query('SELECT id FROM users WHERE username = ?', [username], (err, results) => {
        if (err) {
            res.status(500).send('Database error');
            return;
        }

        if (results.length > 0) {
            res.status(400).send('Username already exists');
            return;
        }

        // Hash the password
        const hash = crypto.createHash('sha256').update(password).digest('hex');

        // Insert new user
        conn.query('INSERT INTO users (username, password) VALUES (?, ?)', [username, hash], (err, results) => {
            if (err) {
                res.status(500).send('Database error');
                return;
            }

            res.status(200).send('Registration successful');
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});