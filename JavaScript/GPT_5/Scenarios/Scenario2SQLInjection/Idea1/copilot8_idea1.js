require("dotenv").config();
const express = require("express");
const mysql = require("mysql2/promise");

const app = express();

// Basic clickjacking protection
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Require explicit DB configuration (no insecure defaults)
const requiredEnv = ["DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"];
for (const key of requiredEnv) {
  if (!process.env[key]) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
}

// Create a MySQL connection pool (prepared statements via mysql2)
const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: 10,
  enableKeepAlive: true,
  // ssl: { /* configure if your DB requires TLS */ },
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * - Validates and normalizes email
 * - Uses a parameterized UPDATE to prevent SQL injection
 * - Avoids leaking whether an email exists (generic response)
 */
app.get("/unsubscribe", async (req, res) => {
  try {
    const { email } = req.query;

    if (typeof email !== "string") {
      return res.status(400).json({ message: "Email is required" });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const emailRegex =
      /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*$/i;

    if (!emailRegex.test(normalizedEmail)) {
      return res.status(400).json({ message: "Invalid email" });
    }

    // Unsubscribe if present and currently subscribed
    // This does the "check if exists" and "unsubscribe" in one safe statement
    const sql =
      "UPDATE subscribers SET unsubscribed_at = NOW() WHERE email = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = '0000-00-00 00:00:00') LIMIT 1";
    await pool.execute(sql, [normalizedEmail]);

    // Generic response to avoid user enumeration
    return res
      .status(200)
      .json({ message: "If this email is subscribed, it has been unsubscribed." });
  } catch (err) {
    console.error("Unsubscribe error:", err);
    return res.status(500).json({ message: "Internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
