const express = require("express");
const mysql = require("mysql");

const requiredEnv = ["DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"];
for (const k of requiredEnv) {
  if (!process.env[k]) {
    console.error(`Missing required env var: ${k}`);
    process.exit(1);
  }
}

const db = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  // Optional hardening flags
  multipleStatements: false,
  supportBigNumbers: true,
  charset: "utf8mb4_general_ci",
});

db.connect((err) => {
  if (err) {
    console.error("DB connection error:", err);
    process.exit(1);
  }
  console.log("Database connected.");
});

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * Note: This uses GET as requested; for state-changing routes, POST with a token is recommended.
 */
app.get("/unsubscribe", (req, res) => {
  const raw = typeof req.query.email === "string" ? req.query.email : "";
  const email = raw.trim().toLowerCase();

  // Strict email validation
  const emailRegex =
    /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/i;

  if (!email || !emailRegex.test(email)) {
    return res.status(400).json({ message: "Invalid or missing email." });
  }

  // Idempotent unsubscribe: only update if currently subscribed
  const unsubscribeSql = `
    UPDATE subscribers
       SET unsubscribed_at = NOW()
     WHERE email = ?
       AND (unsubscribed_at IS NULL OR unsubscribed_at = '0000-00-00 00:00:00')
     LIMIT 1
  `;

  db.query(unsubscribeSql, [email], (updErr, updResult) => {
    if (updErr) {
      console.error("Unsubscribe update error:", updErr);
      return res.status(500).json({ message: "Internal server error." });
    }

    if (updResult.affectedRows > 0) {
      return res.status(200).json({ message: "Successfully unsubscribed." });
    }

    // No row updated: check if the email exists to satisfy the requirement
    const existsSql = "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1";
    db.query(existsSql, [email], (selErr, rows) => {
      if (selErr) {
        console.error("Existence check error:", selErr);
        return res.status(500).json({ message: "Internal server error." });
      }

      if (!rows || rows.length === 0) {
        return res.status(404).json({ message: "Email not found." });
      }

      // Exists but already unsubscribed
      return res.status(200).json({ message: "Already unsubscribed." });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
