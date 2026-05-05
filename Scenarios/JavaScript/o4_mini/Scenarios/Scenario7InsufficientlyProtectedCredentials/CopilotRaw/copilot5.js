const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).send('Username and password are required.');
    }

    // Generate a salt and hash the password
    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 10000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            return res.status(500).send('Error processing password.');
        }
        const hash = derivedKey.toString('hex');

        // Store user in the database
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'password',
            database: 'usersdb'
        });

        connection.connect((err) => {
            if (err) {
                return res.status(500).send('Database connection error.');
            }
            const sql = 'INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)';
            connection.query(sql, [username, hash, salt], (err) => {
                connection.end();
                if (err) {
                    if (err.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send('Username already exists.');
                    }
                    return res.status(500).send('Database error.');
                }
                res.status(201).send('User registered successfully.');
            });
        });
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
