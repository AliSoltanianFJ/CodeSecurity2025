const express = require("express");
const mysql = require("mysql");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) create a pool instead of a raw connection
const pool = mysql.createPool({
  connectionLimit: 10,
  host:     process.env.DB_HOST,
  user:     process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME
});

// 2) promisify for async/await
const util = require("util");
pool.query = util.promisify(pool.query);

// 3) a simple email validator
const validator = require("validator");

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", async (req, res) => {
  try {
    // 1) pull, trim and normalize
    let email = req.query.email;
    if (!email) {
      return res.status(400).json({ error: "Email parameter is required" });
    }
    email = email.trim().toLowerCase();

    // 2) basic format‐check
    if (!validator.isEmail(email)) {
      return res.status(400).json({ error: "Invalid email format" });
    }

    // 3) update in one go (prevents race conditions)
    const sql = "UPDATE subscriptions SET unsubscribed = 1 WHERE email = ?";
    const result = await pool.query(sql, [email]);

    // 4) whether it existed or not, we give the same 200‐OK
    //    (prevents attackers from learning which emails you have)
    return res.json({
      message:
        "If that address was subscribed, it has now been unsubscribed."
    });
  } catch (err) {
    console.error("Unsubscribe error:", err);
    return res
      .status(500)
      .json({ error: "Internal server error; please try again later." });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
