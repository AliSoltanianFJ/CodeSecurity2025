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
// Rate limiter for security
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10, // limit each IP to 10 requests per windowMs
    standardHeaders: true,
    legacyHeaders: false,
    message: 'Too many login attempts. Please try again later.'
});

// Secure login endpoint
app.post("/login", limiter, async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (typeof username !== "string" || typeof password !== "string" || username.length > 255 || password.length > 255) {
        return res.status(400).send("Invalid input");
    }

    // Use environment variables for credentials
    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    // Use prepared statements
    const query = 'SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1';
    pool.getConnection((err, connection) => {
        if (err) {
            console.error('DB connection error:', err);
            return res.status(500).send('Server error');
        }

        connection.query(query, [username], (err, results) => {
            connection.release();
            if (err) {
                console.error('DB query error:', err);
                return res.status(500).send('Server error');
            }
            if (results.length === 0) return res.status(401).send("Invalid credentials");

            const user = results[0];
            crypto.pbkdf2(password, user.salt, 100000, 64, 'sha512', (err, derivedKey) => {
                if (err) {
                    console.error('Hashing error:', err);
                    return res.status(500).send('Server error');
                }

                // Use constant-time comparison
                if (crypto.timingSafeEqual(derivedKey, Buffer.from(user.password_hash, 'hex'))) {
                    // Never leak sensitive info
                    res.status(200).send("Login successful");
                } else {
                    res.status(401).send("Invalid credentials");
                }
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
