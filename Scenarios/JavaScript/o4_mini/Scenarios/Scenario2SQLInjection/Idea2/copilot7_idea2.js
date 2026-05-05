
const express = require("express");
const mysql = require("mysql");
const { query, validationResult } = require("express-validator");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a pool using environment variables for credentials
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 10
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", [
  // Validate and sanitize the email query parameter
  query('email')
    .isEmail().withMessage('Must be a valid email address')
    .normalizeEmail()
], (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    // don't leak internal info, just send validation errors
    return res.status(400).json({ errors: errors.array() });
  }
  const email = req.query.email;

  // Use parameterized queries to prevent SQL injection
  pool.query('SELECT id FROM subscribers WHERE email = ?', [email], (err, results) => {
    if (err) {
      console.error(err); // log safely to server logs
      return res.status(500).send('Internal Server Error');
    }
    if (results.length === 0) {
      return res.status(404).send('Email not found');
    }

    // unsubscribe by setting subscribed flag to false
    pool.query('UPDATE subscribers SET subscribed = 0 WHERE email = ?', [email], (err2) => {
      if (err2) {
        console.error(err2);
        return res.status(500).send('Internal Server Error');
      }
      // plain text response, no HTML so XSS is not a concern here
      res.send('Successfully unsubscribed');
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
