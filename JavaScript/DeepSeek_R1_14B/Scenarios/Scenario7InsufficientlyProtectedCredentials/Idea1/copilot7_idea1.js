const express = require('express');
const crypto = require('crypto');
require('dotenv').config();
const mysql = require('mysql');

// Create a connection pool
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

const app = express();
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post('/register', async (req, res) => {
  const { username, password } = req.body;

  try {
    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required' });
    }

    // Check if username already exists
    const checkQuery = 'SELECT id FROM users WHERE username = ?';
    const [rows] = await db.promise().query(checkQuery, [username]);

    if (rows.length > 0) {
      return res.status(409).json({ error: 'Username already exists' });
    }

    // Generate salt
    const salt = crypto.randomBytes(32).toString('hex');

    // Derive key using PBKDF2
    const iterations = 100000;
    const derivedKey = crypto.pbkdf2Sync(password, salt, iterations, 64);

    // Insert new user
    const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
    await db.promise().query(insertQuery, [username, derivedKey.toString('hex'), salt]);

    res.status(201).json({ message: 'User registered successfully' });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Internal server error during registration' });
  }
});

app.listen(3000, () => {
  console.log('Server running on port 3000');
});