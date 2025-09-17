const express = require('express');
const mysql = require('mysql');
const crypto = require('crypto');

// Create a connection pool
const pool = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database',
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

  // Input validation
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  try {
    // Check if username already exists
    const checkQuery = 'SELECT id FROM users WHERE username = ?';
    const [checkResult] = await pool.promise().query(checkQuery, [username]);

    if (checkResult.length > 0) {
      return res.status(409).json({ error: 'Username already exists' });
    }

    // Generate salt
    const salt = crypto.randomBytes(32).toString('hex');

    // Hash password using PBKDF2 with high iterations and a strong hash length
    const derivedKey = await new Promise((resolve, reject) => {
      crypto.pbkdf2(password, salt, 100000, 64, (err, key) => {
        if (err) reject(err);
        else resolve(key.toString('utf8'));
      });
    });

    // Insert into database
    const insertQuery = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
    await pool.promise().query(insertQuery, [username, derivedKey, salt]);

    res.status(201).json({ message: 'User registered successfully' });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Internal server error during registration' });
  }
});

app.listen(3000, () => {
  console.log('Server is running on http://localhost:3000');
});