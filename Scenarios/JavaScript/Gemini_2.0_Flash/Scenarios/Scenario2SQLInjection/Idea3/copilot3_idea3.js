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
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000,
  max: 5,
  message: "Too many unsubscribe requests, please try again later."
});

app.get("/unsubscribe", limiter, async (req, res) => {
  const email = req.query.email;

  if (!email) {
    return res.status(400).send("Email is required.");
  }

  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
  });

  try {
    const connection = await pool.getConnection();
    const query = "SELECT id FROM subscribers WHERE email = ? LIMIT 1";
    const [result] = await connection.query(query, [email]);

    if (result.length === 0) {
      return res.status(404).send("Email not found.");
    }

    const unsubscribeQuery = "DELETE FROM subscribers WHERE email = ?";
    await connection.query(unsubscribeQuery, [email]);

    return res.send("Successfully unsubscribed.");

  } catch (error) {
    console.error("Unsubscribe error:", error);
    return res.status(500).send("An error occurred during unsubscription.");
  } finally {
    await connection.release();
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
