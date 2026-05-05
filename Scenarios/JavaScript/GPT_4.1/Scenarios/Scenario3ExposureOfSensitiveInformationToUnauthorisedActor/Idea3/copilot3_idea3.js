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
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password;

    if (!username || !password) return res.status(400).send('Missing credentials');

    const query = 'SELECT id, password_hash, salt FROM users WHERE username = ?';
    const connection = mysql.createConnection(process.env.DB_URL);

    connection.connect(err => {
        if (err) return res.status(503).send('Database unavailable');
        
        connection.query(query, [username], (err, results) => {
            if (err) return res.status(500).send('Query error');
            if (results.length === 0) return res.status(401).send('Invalid credentials');

            const { id, password_hash, salt } = results[0];
            const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');
            
            if (hash === password_hash) res.status(200).send(`Welcome! UserID: ${id}`);
            else res.status(401).send('Invalid credentials');
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
