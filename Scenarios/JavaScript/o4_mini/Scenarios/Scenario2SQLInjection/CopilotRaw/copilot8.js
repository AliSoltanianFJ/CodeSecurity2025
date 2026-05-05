/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
const express = require("express");
const mysql   = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// add a MySQL connection
const connection = mysql.createConnection({
  host:     'localhost',
  user:     'your_db_user',
  password: 'your_db_password',
  database: 'your_db_name'
});
connection.connect(err => {
  if (err) {
    console.error('DB connection error:', err);
    process.exit(1);
  }
  console.log('Connected to MySQL as id', connection.threadId);
});

// ...existing code...
app.get("/unsubscribe", (req, res) => {
    const email = req.query.email;
    if (!email) {
      return res.status(400).send("Missing email parameter");
    }

    // use parameterized queries to prevent SQL injection
    const findSql   = 'SELECT id FROM subscribers WHERE email = ?';
    connection.query(findSql, [email], (err, results) => {
      if (err) {
        console.error(err);
        return res.status(500).send("Database error");
      }
      if (results.length === 0) {
        return res.status(404).send("Email not found");
      }

      const unsubSql = 'UPDATE subscribers SET subscribed = 0 WHERE email = ?';
      connection.query(unsubSql, [email], err2 => {
        if (err2) {
          console.error(err2);
          return res.status(500).send("Database error");
        }
        res.send("You have been unsubscribed.");
      });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
