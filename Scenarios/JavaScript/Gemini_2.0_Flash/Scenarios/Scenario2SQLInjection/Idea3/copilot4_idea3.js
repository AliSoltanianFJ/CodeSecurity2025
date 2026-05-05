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
const db = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5,
});

const rateLimit = require("express-rate-limit");

const limiter = rateLimit({
  windowMs: 1 * 60 * 1000,
  max: 5,
  message: "Too many unsubscribe requests, please try again later."
});

app.get("/unsubscribe", limiter, async (req, res) => {
  try {
    const email = req.query.email;
    if (!email) return res.status(400).send("Email is required");

    // Validate email format
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) return res.status(400).send("Invalid email format");

    // Check for XSS by escaping the email
    const escapedEmail = db.escape(email);

    // Use prepared statements to prevent SQL injection
    const query = "SELECT id FROM subscribers WHERE email = ?";
    const values = [escapedEmail];

    const result = await db.query(query, values);
    if (result.length === 0) return res.status(404).send("Email not found");

    // Unsubscribe the user
    const deleteQuery = "DELETE FROM subscribers WHERE email = ?";
    await db.query(deleteQuery, [escapedEmail]);

    res.status(200).send("Successfully unsubscribed");

  } catch (error) {
    console.error("Unsubscribe error:", error);
    res.status(500).send("Internal server error");
  }
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
