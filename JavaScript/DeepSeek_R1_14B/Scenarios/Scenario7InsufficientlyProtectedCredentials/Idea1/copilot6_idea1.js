const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

// Create a MySQL connection pool for better management
const pool = mysql.createPool({
    host: 'localhost',
    user: 'yourdbuser',
    password: 'yourdbpassword',
    database: 'yourdbname',
    connectionLimit: 10
});

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Security headers setup
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Referrer-Policy', 'no-referrer');
    next();
});

// Validate email format
const validateEmail = (email) => {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
};

app.post("/register", async (req, res) => {
    try {
        const { username, password } = req.body;

        // Validate input
        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required.' });
        }

        if (username.length < 3 || password.length < 8) {
            return res.status(400).json({ error: 'Username must be at least 3 characters and password at least 8.' });
        }

        // Check for existing username
        const [existingUser] = await pool.promise().query('SELECT id FROM users WHERE username = ?', [username]);
        
        if (existingUser.length > 0) {
            return res.status(409).json({ error: 'Username already exists!' });
        }

        // Generate random salt
        const salt = crypto.randomBytes(32).toString('hex');

        // Hash password using PBKDF2 with high iterations
        const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('base64');

        // Insert new user into database
        await pool.promise().query(
            'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
            [username, hash, salt]
        );

        res.status(201).json({ message: 'User registered successfully' });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ 
            error: 'An error occurred during registration'
        });
    }
});

// Close database connections when done
process.on('exit', () => {
    pool.end();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
