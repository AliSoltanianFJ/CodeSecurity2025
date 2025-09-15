const express = require("express");
const crypto = require('crypto');
const mysql = require("mysql");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

app.post("/login", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    try {
        // Generate salt and hash for the provided password
        const salt = crypto.randomBytes(16).toString('hex');
        const iterations = 1000; // PBKDF2 iterations
        
        const derivedKey = crypto.pbkdf2Sync(password, salt, iterations, 64, 'sha512');
        const hash = derivedKey.toString('hex');

        // Query the database for user existence
        let [rows] = await db.query(
            'SELECT id, password_hash, salt FROM users WHERE username = ?',
            [username]
        );

        if (rows.length === 0) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }

        // Verify the stored hash and salt match the provided password
        const storedSalt = rows[0].salt;
        const storedHash = rows[0].password_hash;

        const verifiedKey = crypto.pbkdf2Sync(password, storedSalt, iterations, 64, 'sha512').toString('hex');

        if (storedHash === verifiedKey) {
            // Generate a token for authentication
            const token = crypto.randomBytes(32).toString('hex');

            req.session.token = token;
            res.status(200).json({ 
                success: true, 
                message: 'Login successful!', 
                token 
            });
        } else {
            res.status(401).json({ message: 'Invalid password' });
        }
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ message: 'Server error during login' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});