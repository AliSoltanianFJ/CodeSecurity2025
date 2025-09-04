const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code above...
const session = require('express-session');
const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME
});

app.use(session({
    secret: process.env.SESSION_SECRET,
    resave: false,
    saveUninitialized: false,
    cookie: {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production'
    }
}));

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    const passwordHash = crypto.createHash('sha256').update(password).digest('hex');
    pool.query(
        'SELECT id FROM users WHERE username = ? AND password_hash = ?',
        [username, passwordHash],
        (err, results) => {
            if (err) {
                console.error('Database error during login:', err);
                return res.status(500).send('Internal server error');
            }
            if (results.length === 0) {
                return res.status(401).send('Invalid username or password');
            }
            req.session.userId = results[0].id;
            res.send('Login successful');
        }
    );
});
// ...existing code below...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
