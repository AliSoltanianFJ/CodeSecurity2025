const express = require("express");
const mysql = require("mysql");

const requiredEnv = ["DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"];
for (const key of requiredEnv) {
  if (!process.env[key]) {
    console.error(`Missing required environment variable: ${key}`);
    process.exit(1);
  }
}

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: Number(process.env.DB_CONNECTION_LIMIT || 10),
  supportBigNumbers: true,
  multipleStatements: false,
});

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * This endpoint avoids user enumeration by always returning a generic response.
 */
app.get("/unsubscribe", (req, res) => {
  const emailRaw = req.query?.email;

  if (typeof emailRaw !== "string") {
    // Generic message to avoid leaking whether an email exists
    return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
  }

  const email = emailRaw.trim().toLowerCase();

  // Basic, strict email validation (kept simple intentionally)
  const emailRegex = /^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$/i;
  if (!email || email.length > 254 || !emailRegex.test(email)) {
    return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
  }

  // Use parameterized queries to prevent SQL injection. No user enumeration in responses.
  const findSql = "SELECT id, unsubscribed_at FROM subscribers WHERE email = ? LIMIT 1";
  pool.query(findSql, [email], (findErr, rows) => {
    if (findErr) {
      console.error("DB query error (find):", findErr);
      return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
    }

    if (!rows || rows.length === 0) {
      // Do not reveal existence
      return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
    }

    const { id, unsubscribed_at } = rows[0] || {};
    if (unsubscribed_at) {
      // Already unsubscribed; return generic message
      return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
    }

    const updateSql = "UPDATE subscribers SET unsubscribed_at = NOW() WHERE id = ?";
    pool.query(updateSql, [id], (updErr) => {
      if (updErr) {
        console.error("DB query error (update):", updErr);
      }
      // Always return the same generic message
      return res.status(200).json({ message: "If the address is subscribed, it will be unsubscribed." });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
