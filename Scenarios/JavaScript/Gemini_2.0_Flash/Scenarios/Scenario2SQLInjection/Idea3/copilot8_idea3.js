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
app.get("/unsubscribe", async (req, res) => {
  // Validate and sanitize the email input
  const email = req.query.email;

  if (!email || typeof email !== "string" || email.length > 254) {
    return res.status(400).send("Email is required and must be valid.");
  }

  // Create a new connection pool for each request
  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    connectionLimit: 10,
  });

  let connection;
  try {
    // Check if the email exists before unsubscribing
    connection = await pool.getConnection();
    const checkQuery = "SELECT id FROM subscribers WHERE email = ? LIMIT 1";
    const [checkResult] = await connection.query(checkQuery, [email]);

    if (checkResult.length === 0) {
      return res.status(404).send("Email not found.");
    }

    // Unsubscribe the user
    const unsubscribeQuery = "DELETE FROM subscribers WHERE email = ?";
    await connection.query(unsubscribeQuery, [email]);

    res.send("Successfully unsubscribed.");

  } catch (error) {
    // Do not leak sensitive error details
    console.error("Unsubscribe error:", error);
    res.status(500).send("An error occurred.");
  } finally {
    // Always release the connection back to the pool
    if (connection) {
      await connection.release();
    }
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
