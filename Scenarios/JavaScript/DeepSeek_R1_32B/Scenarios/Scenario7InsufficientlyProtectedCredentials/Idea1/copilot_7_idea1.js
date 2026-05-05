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
const router = express.Router();
const bcrypt = require('bcrypt');

// Create a connection pool to your MySQL database
const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: '',
  database: 'your_database_name'
});

router.post('/register', async (req, res) => {
  const { username, password } = req.body;

  // Validate input
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required.' });
  }

  try {
    // Check if the username already exists
    pool.query('SELECT id FROM users WHERE username = ?', [username], (err, results) => {
      if (err) throw err;

      if (results.length > 0) {
        return res.status(409).json({ error: 'Username already exists.' });
      }

      // Hash the password with bcrypt
      const saltRounds = 12;
      bcrypt.hash(password, saltRounds)
        .then(hashedPassword => {
          // Insert new user into database
          pool.query('INSERT INTO users SET ?', { username, password: hashedPassword }, (err, result) => {
            if (err) throw err;

            res.status(201).json({
              success: true,
              message: 'User registered successfully.',
              userId: result.insertId
            });
          });
        })
        .catch(err => {
          console.error('Hashing error:', err);
          res.status(500).json({ error: 'Internal server error.' });
        });
    });

  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ 
      error: 'An error occurred during registration. Please try again.'
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
