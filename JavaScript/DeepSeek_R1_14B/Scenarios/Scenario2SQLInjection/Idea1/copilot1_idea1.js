const express = require('express');
const mysql = require('mysql');
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create database connection pool
const db = mysql.createPool({
  host: 'localhost',
  user: 'your_user',
  password: 'your_password',
  database: 'your_database',
  port: 3306,
  connectionLimit: 10
});

app.get('/unsubscribe', async (req, res) => {
  const email = req.query.email;

  if (!email) {
    return res.status(400).json({ error: "Email is required" });
  }

  try {
    await db.promise()
      .query('SELECT id FROM users WHERE email = ?', [email])
      .then(results => {
        if (results.length === 0) {
          throw new Error("Email not found");
        }
        
        return db.promise()
          .execute('UPDATE users SET subscribed = 0 WHERE email = ?', [email]);
      })
      .then(() => {
        res.status(200).json({ message: "Successfully unsubscribed" });
      });

  } catch (error) {
    console.error('Error:', error);
    res.status(500).json({ 
      error: process.env.NODE_ENV === 'development' ? error.message : 'Something went wrong'
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
