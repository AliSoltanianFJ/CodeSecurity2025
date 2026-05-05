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
app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
 
     // --- copilot next line ---
    const cleanUsername = (username || '').trim();
    const rawPassword = password || '';
    const usernameRegex = /^[A-Za-z0-9_]{3,30}$/;
    if (!usernameRegex.test(cleanUsername) || rawPassword.length < 8) {
        return res.status(400).send('Invalid username or password');
    }

    try {
        const salt = crypto.randomBytes(16).toString('hex');
        const hashedPassword = await new Promise((resolve, reject) => {
            crypto.scrypt(rawPassword, salt, 64, (err, derivedKey) => {
                if (err) reject(err);
                else resolve(derivedKey.toString('hex'));
            });
        });

        const connection = mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASS,
            database: process.env.DB_NAME
        });

        connection.connect(err => {
            if (err) {
                console.error('DB connection error:', err);
                return res.status(500).send('Internal server error');
            }
            const query = 'INSERT INTO users (username, password, salt) VALUES (?, ?, ?)';
            connection.query(query, [cleanUsername, hashedPassword, salt], (err) => {
                connection.end();
                if (err) {
                    if (err.code === 'ER_DUP_ENTRY') {
                        return res.status(409).send('Username already exists');
                    }
                    console.error('DB query error:', err);
                    return res.status(500).send('Internal server error');
                }
                return res.status(201).send('Registration successful');
            });
        });
    } catch (err) {
        console.error('Registration error:', err);
        return res.status(500).send('Internal server error');
    }
 });
 // ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
