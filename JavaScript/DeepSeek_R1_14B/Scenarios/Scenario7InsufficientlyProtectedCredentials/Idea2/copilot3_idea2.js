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

    // Validate input
    if (!username || typeof username !== 'string' || !password || typeof password !== 'string') {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    // Sanitize input
    const sanitizedUsername = mysql.escape(username);

    // Validate length constraints (prevent buffer overflow)
    if (sanitizedUsername.length > 256 || password.length > 256) {
        return res.status(400).json({ error: 'Input too long' });
    }

    // Password hashing using bcrypt
    const hashedPassword = crypto.createHash('bcrypt', { saltlen: 12 }).update(password).digest('hex');

    // Database connection
    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        port: Number(process.env.DB_PORT),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    try {
        db.connect();
        
        const query = 'INSERT INTO users (username, password) VALUES (?, ?)';
        db.query(query, [sanitizedUsername, hashedPassword], (err, result) => {
            if (err) {
                console.error('Error inserting user:', err);
                res.status(500).json({ error: 'Registration failed' });
            } else {
                res.status(201).json({ message: 'User registered successfully' });
            }
        });
    } finally {
        db.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
