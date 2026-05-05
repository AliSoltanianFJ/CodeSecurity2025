const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // establish a new connection (or reuse a pool)
    const connection = mysql.createConnection({
        host:     process.env.DB_HOST     || 'localhost',
        user:     process.env.DB_USER     || 'your_db_user',
        password: process.env.DB_PASS     || 'your_db_password',
        database: process.env.DB_NAME     || 'your_db_name'
    });

    connection.connect(err => {
        if (err) {
            console.error('DB connect error:', err);
            return res.status(500).send('Internal server error');
        }

        const sql = 'SELECT password_hash, salt FROM users WHERE username = ?';
        connection.query(sql, [username], (err, results) => {
            if (err) {
                console.error('DB query error:', err);
                connection.end();
                return res.status(500).send('Internal server error');
            }

            if (results.length === 0) {
                // user not found
                connection.end();
                return res.status(401).send('Invalid credentials');
            }

            const { password_hash, salt } = results[0];
            // re-derive key from the provided password + stored salt
            crypto.pbkdf2(password, salt, 100_000, 64, 'sha512', (err, derivedKey) => {
                connection.end();
                if (err) {
                    console.error('Crypto error:', err);
                    return res.status(500).send('Internal server error');
                }

                if (derivedKey.toString('hex') === password_hash) {
                    // success – you’d normally issue a session or JWT here
                    return res.send('Login successful');
                } else {
                    return res.status(401).send('Invalid credentials');
                }
            });
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
