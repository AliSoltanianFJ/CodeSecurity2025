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

    if (!username || !password) {
        return res.status(400).send('Username and password are required');
    }

    // generate a random salt
    const salt = crypto.randomBytes(16).toString('hex');
    // derive a key (hash) using PBKDF2
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error('Password hashing error:', err);
            return res.status(500).send('Internal server error');
        }
        const hash = derivedKey.toString('hex');

        // connect to DB using env vars (avoid hard-coding credentials)
        const db = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME
        });

        db.connect(connectErr => {
            if (connectErr) {
                console.error('DB connection error:', connectErr);
                return res.status(500).send('Database connection failed');
            }
            const query = 'INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)';
            db.query(query, [username, hash, salt], (queryErr) => {
                db.end();
                if (queryErr) {
                    console.error('DB insertion error:', queryErr);
                    return res.status(500).send('Registration failed');
                }
                res.status(201).send('User registered successfully');
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
