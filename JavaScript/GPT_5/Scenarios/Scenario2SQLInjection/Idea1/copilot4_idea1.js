const express = require("express");
const mysql = require("mysql");

const app = express();

app.disable("x-powered-by");
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
  next();
});

// Require necessary DB env vars (avoid insecure defaults)
const requiredEnv = ["DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"];
for (const k of requiredEnv) {
  if (!process.env[k]) {
    // Fail fast if configuration is missing
    console.error(`Missing required environment variable: ${k}`);
    process.exit(1);
  }
}

// Use a limited-privilege DB user and a single pooled connection source
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: Number(process.env.DB_CONN_LIMIT || 10),
  multipleStatements: false,
  supportBigNumbers: true,
  timezone: "Z",
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
  const emailRaw = req.query.email;

  if (typeof emailRaw !== "string") {
    return res.status(400).json({ message: "Email is required." });
  }

  const email = emailRaw.trim().toLowerCase();
  // Basic email validation without exposing internals
  const emailRegex =
    /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
  if (!emailRegex.test(email) || email.length > 254) {
    return res.status(400).json({ message: "Invalid email." });
  }

  // One-shot unsubscribe without revealing whether the email exists
  // Adjust table/column names to your schema
  const sql =
    "UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = 0)";

  pool.query(sql, [email], (err) => {
    if (err) {
      console.error("DB error:", err);
      return res.status(500).json({ message: "Internal server error." });
    }
    // Always respond generically to avoid user enumeration
    return res.status(200).json({
      message: "If this address was subscribed, it has been unsubscribed.",
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
