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
  connectionLimit: Number(process.env.DB_CONN_LIMIT || 10),
  connectTimeout: 10000,
  acquireTimeout: 10000,
  charset: "utf8mb4_general_ci",
  ssl: process.env.DB_SSL === "true"
    ? {
        rejectUnauthorized: process.env.DB_SSL_REJECT_UNAUTHORIZED !== "false",
        ca: process.env.DB_SSL_CA || undefined,
      }
    : undefined,
});

const app = express();
app.disable("x-powered-by");

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * This endpoint returns a generic success message to avoid user enumeration.
 */
app.get("/unsubscribe", (req, res) => {
  const raw = req.query.email;

  if (typeof raw !== "string") {
    return res.status(400).json({ message: "Email is required." });
  }

  const email = raw.trim().toLowerCase();

  // Basic, strict email validation and size limits
  if (email.length < 3 || email.length > 254) {
    return res.status(400).json({ message: "Invalid email." });
  }
  const emailRegex =
    /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)+$/i;
  if (!emailRegex.test(email)) {
    return res.status(400).json({ message: "Invalid email." });
  }

  // Single statement to avoid a prior existence check
  const sql =
    "UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = 0)";

  pool.query(sql, [email], (err) => {
    if (err) {
      console.error("Database error:", err);
      return res.status(500).json({ message: "Internal server error." });
    }
    // Do not reveal if the email existed or was already unsubscribed
    return res.status(200).json({
      message:
        "If the email is subscribed, it has been unsubscribed.",
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
