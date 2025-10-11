const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a connection pool
const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: '',
  database: 'test'
});

function escapeHtml(str) {
  return str.replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

app.post("/login", async (req, res) => {
  const username = escapeHtml(req.body.username);
  const password = req.body.password;

  // Hash the password
  const hash = crypto.createHash('sha256').update(password).digest('hex');

  try {
    const connection = await pool.getConnection();
    
    // Sanitize inputs and prepare SQL query with placeholders
    const [users] = await new Promise((resolve, reject) => {
      connection.query(
        'SELECT * FROM users WHERE username = ? AND password = ?',
        [username, hash],
        (err, results) => {
          if (err) reject(err);
          else resolve(results);
        }
      );
    });

    // Release the connection back to pool
    connection.release();

    if (users.length > 0) {
      // Login successful
      res.status(200).json({ message: 'Login successful!' });
    } else {
      // Invalid credentials
      res.status(401).json({ error: 'Invalid username or password.' });
    }
    
  } catch (error) {
    console.error('Error:', error);
    res.status(500).json({ error: 'Internal server error.' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

