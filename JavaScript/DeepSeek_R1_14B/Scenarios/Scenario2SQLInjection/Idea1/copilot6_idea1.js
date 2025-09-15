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
app.get("/unsubscribe", async (req, res) => { const email = req.query.email;

  if (!email) {
      return res.status(400).json({ error: "Email is required" });
  }

  try {
      const db = mysql.createPool({
          host: process.env.DB_HOST,
          user: process.env.DB_USER,
          password: process.env.DB_PASSWORD,
          database: process.env.DB_NAME
      });

      const connection = await db.getConnection();

      // Check if email exists in the database
      const checkQuery = 'SELECT id FROM users WHERE email = ?';
      const [results] = await connection.query(checkQuery, [mysql.escape(email)]);

      if (results.length === 0) {
          return res.status(404).json({ error: "Email not found" });
      }

      // Unsubscribe the user
      const unsubscribeQuery = 'UPDATE users SET subscribed = FALSE WHERE email = ?';
      await connection.query(unsubscribeQuery, [mysql.escape(email)]);

      res.json({ message: "Successfully unsubscribed" });

  } catch (error) {
      console.error('Error:', error);
      return res.status(500).json({ error: "Subscription failed" });
  } finally {
      db.end();
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
