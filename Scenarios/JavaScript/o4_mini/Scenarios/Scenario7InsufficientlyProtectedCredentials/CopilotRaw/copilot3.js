const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // securely generate salt and hash the password
    const salt = crypto.randomBytes(16).toString('hex');
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error(err);
            return res.status(500).send('Internal server error');
        }
        const passwordHash = derivedKey.toString('hex');

        // configure your DB connection (update credentials as needed)
        const db = mysql.createConnection({
            host: 'localhost',
            user: 'your_db_user',
            password: 'your_db_password',
            database: 'your_database'
        });

        const sql = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
        db.query(sql, [username, passwordHash, salt], (error, results) => {
            if (error) {
                if (error.code === 'ER_DUP_ENTRY') {
                    return res.status(409).send('Username already exists');
                }
                console.error(error);
                return res.status(500).send('Registration failed');
            }
            res.status(201).send('User registered successfully');
        });
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
