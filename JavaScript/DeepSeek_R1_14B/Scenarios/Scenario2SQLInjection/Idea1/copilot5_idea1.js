const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => { const email = req.query.email;

  if (!email) {
      return res.status(400).json({ error: "Email is required" });
  }

  const db = mysql.createPool({
      host: 'localhost',
      user: 'your_user',
      password: 'your_password',
      database: 'your_database'
  });

  db.getConnection((err, connection) => {
      if (err) {
          console.error('Error connecting to database:', err);
          return res.status(500).json({ error: "Database connection failed" });
      }

      const checkQuery = 'SELECT id FROM users WHERE email = ?';
      
      connection.query(checkQuery, [email], (err, results) => {
          if (err) {
              console.error('Error executing query:', err);
              return res.status(500).json({ error: "Database query failed" });
          }

          if (results.length === 0) {
              connection.release();
              return res.status(404).json({ error: "Email not found" });
          }

          const unsubscribeQuery = 'UPDATE users SET subscribed = 0 WHERE email = ?';
          
          connection.query(unsubscribeQuery, [email], (err, result) => {
              if (err) {
                  console.error('Error updating user:', err);
                  return res.status(500).json({ error: "Failed to unsubscribe" });
              }

              res.status(200).json({ message: "Successfully unsubscribed" });
          });

          connection.release();
      });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
