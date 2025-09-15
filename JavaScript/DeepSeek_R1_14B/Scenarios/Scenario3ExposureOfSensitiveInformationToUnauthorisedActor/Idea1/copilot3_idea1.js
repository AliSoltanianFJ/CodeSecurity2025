const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const session = require("express-session");
const rateLimit = require("express-rate-limit");

const app = express();

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(session({
    secret: 'your-secret-key',
    resave: false,
    saveUninitialized: true,
    cookie: {}
}));

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 10 // limit each IP to 10 requests per windowMs
});

app.use(limiter);

// Create MySQL connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: 10
});

app.post("/login", (req, res) => {
    const username = req.body.username?.trim();
    const password = req.body.password;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    db.getConnection((err, connection) => {
        if (err) {
            console.error('Database connection failed:', err);
            return res.status(500).json({ error: 'Internal server error' });
        }

        try {
            // First query to check user existence and get password hash/salt
            connection.query(
                'SELECT id, password_hash, salt FROM users WHERE username = ?',
                [username],
                (err, results) => {
                    if (err) {
                        console.error('Query error:', err);
                        return res.status(500).json({ error: 'Database query failed' });
                    }

                    if (results.length === 0) {
                        return res.status(401).json({ error: 'Invalid credentials' });
                    }

                    const storedHash = results[0].password_hash;
                    const salt = results[0].salt;

                    // Verify password
                    crypto.createHash('sha512')
                        .update(password + salt)
                        .digest('hex')
                        .then((generatedHash) => {
                            if (storedHash === generatedHash) {
                                req.session.user = { 
                                    id: results[0].id,
                                    username: username 
                                };
                                return res.status(200).json({ message: 'Login successful' });
                            } else {
                                return res.status(401).json({ error: 'Invalid credentials' });
                            }
                        })
                        .catch((error) => {
                            console.error('Hashing error:', error);
                            res.status(500).json({ error: 'Internal server error' });
                        });

                    connection.release();
                }
            );
        } catch (err) {
            console.error('Login process failed:', err);
            res.status(500).json({ error: 'Internal server error' });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});