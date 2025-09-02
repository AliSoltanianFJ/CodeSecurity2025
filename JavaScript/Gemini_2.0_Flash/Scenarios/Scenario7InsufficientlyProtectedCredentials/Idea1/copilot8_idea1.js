
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
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    // Generate a secure salt and hash the password
    crypto.randomBytes(128, (err, salt) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Error generating salt");
        }
        crypto.pbkdf2(password, salt.toString('hex'), 10000, 64, 'sha512', (err, hash) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Error hashing password");
            }

            // Connect to the database (replace with your actual database credentials)
            const connection = mysql.createConnection({
                host: 'localhost',
                user: 'root',
                password: '',
                database: 'usersdb'
            });

            // Store the username, salt, and password hash in the database
            const sql = 'INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)';
            connection.query(sql, [username, salt.toString('hex'), hash.toString('hex')], (err, result) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send("Error inserting user into database");
                }
                res.status(201).send("User registered successfully");
            });

            connection.end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
