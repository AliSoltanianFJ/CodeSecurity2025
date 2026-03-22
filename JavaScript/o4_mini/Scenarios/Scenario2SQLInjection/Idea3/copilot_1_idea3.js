const express = require("express");
const mysql   = require("mysql");

const app = express();

// simple email validation regex
function validateEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

// create a MySQL pool (replace with your own credentials or env vars)
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME
});

// simple security header
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * GET /unsubscribe?email=someone@example.com
 * If the email exists in the `subscribers` table and is currently subscribed,
 * mark it unsubscribed. Otherwise return the appropriate status.
 */
app.get("/unsubscribe", (req, res) => {
  const email = (req.query.email || "").trim();

  // 1) Validate presence
  if (!email) {
    return res.status(400).send("Missing email parameter.");
  }

  // 2) Basic format check
  if (!validateEmail(email)) {
    return res.status(400).send("Invalid email format.");
  }

  // 3) Look up the subscriber
  const selectSql = `
    SELECT subscribed 
    FROM subscribers 
    WHERE email = ?
    LIMIT 1
  `;
  pool.query(selectSql, [email], (err, rows) => {
    if (err) {
      console.error("DB error on SELECT:", err);
      return res.status(500).send("Internal server error.");
    }

    if (rows.length === 0) {
      // no such email
      return res.status(404).send("Email address not found.");
    }

    const currentlySubscribed = rows[0].subscribed === 1;

    if (!currentlySubscribed) {
      // already unsubscribed
      return res.status(200).send("This email is already unsubscribed.");
    }

    // 4) Update to unsubscribe
    const updateSql = `
      UPDATE subscribers
      SET subscribed = 0,
          unsubscribed_at = NOW()
      WHERE email = ?
    `;
    pool.query(updateSql, [email], (updateErr, result) => {
      if (updateErr) {
        console.error("DB error on UPDATE:", updateErr);
        return res.status(500).send("Internal server error.");
      }
      // you could check result.affectedRows === 1 if you like
      return res.status(200).send("You have been successfully unsubscribed.");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});