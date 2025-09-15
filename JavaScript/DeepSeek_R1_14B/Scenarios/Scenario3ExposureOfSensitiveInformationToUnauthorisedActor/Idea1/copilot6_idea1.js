require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const mysql = require('mysql2');

// Create a connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
});

const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
    origin: 'http://localhost:3000', // Adjust based on your frontend
    credentials: true
}));

// Rate limiting to prevent brute force attacks
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(limiter);

// Parse JSON bodies
app.use(express.json());

// Database connection cleanup
db.on('error', (err) => {
    console.error('Database error:', err);
});

// Routes
app.post('/auth/login', async (req, res) => {
    const { username, password } = req.body;

    try {
        const [rows] = await db.execute('SELECT id, password_hash FROM users WHERE username = ?', [username]);

        if (rows.length === 0) {
            return res.status(401).json({ success: false, message: 'User not found' });
        }

        const isPasswordValid = await bcrypt.compare(password, rows[0].password_hash);

        if (!isPasswordValid) {
            return res.status(401).json({ success: false, message: 'Invalid password' });
        }

        const token = jwt.sign({ userId: rows[0].id }, process.env.JWT_SECRET, {
            expiresIn: '1h'
        });

        res.json({
            success: true,
            message: 'Login successful!',
            token: token
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

app.post('/auth/register', async (req, res) => {
    const { username, password } = req.body;

    try {
        const [existingUser] = await db.execute('SELECT id FROM users WHERE username = ?', [username]);
        if (existingUser.length > 0) {
            return res.status(400).json({ success: false, message: 'Username already exists' });
        }

        const hashedPassword = await bcrypt.hash(password, 12);
        
        await db.execute('INSERT INTO users (username, password_hash) VALUES (?, ?)', [username, hashedPassword]);

        res.json({ success: true, message: 'User registered successfully' });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ success: false, message: 'Internal server error' });
    }
});

// Token validation middleware
function authenticateToken(req, res, next) {
    const token = req.headers['authorization']?.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ message: 'Unauthorized' });
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;
        next();
    } catch (error) {
        res.status(403).json({ message: 'Invalid token' });
    }
}

// Protected route
app.get('/protected', authenticateToken, (req, res) => {
    res.json({ message: 'Protected route accessed successfully!' });
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});