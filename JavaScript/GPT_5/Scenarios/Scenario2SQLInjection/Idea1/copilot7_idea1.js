const express = require("express");
const mysql = require("mysql2/promise");

const app = express();

app.disable("x-powered-by");
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Require DB configuration explicitly (no insecure defaults)
function requireEnv(name) {
  const v = process.env[name];
  if (!v) throw new Error(`Missing required env var: ${name}`);
  return v;
}

const pool = mysql.createPool({
  host: requireEnv("DB_HOST"),
  user: requireEnv("DB_USER"),
  password: requireEnv("DB_PASSWORD"),
  database: requireEnv("DB_NAME"),
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
});

// Verify DB connectivity on startup
(async () => {
  try {
    await pool.query("SELECT 1");
    // eslint-disable-next-line no-console
    console.log("Database connection OK");
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("Failed to connect to database:", err);
    process.exit(1);
  }
})();

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 * - Validates and normalizes email
 * - Uses a single parameterized UPDATE to avoid injection and enumeration
 * - Returns a generic message to avoid disclosing subscription status
 */
app.get("/unsubscribe", async (req, res) => {
  try {
    const raw = req.query.email;

    if (typeof raw !== "string") {
      return res.status(400).json({ message: "Email is required" });
    }

    const email = raw.trim().toLowerCase();
    // Basic validation and length limits
    if (email.length === 0 || email.length > 254) {
      return res.status(400).json({ message: "Invalid email" });
    }
    const emailRegex =
      /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*$/i;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: "Invalid email" });
    }

    // Idempotent unsubscribe: only updates if a matching, currently subscribed row exists
    const [result] = await pool.execute(
      `
      UPDATE subscribers
      SET unsubscribed_at = UTC_TIMESTAMP()
      WHERE email = ? AND (unsubscribed_at IS NULL OR unsubscribed_at = '0000-00-00 00:00:00')
      `,
      [email]
    );

    // Do not reveal whether the email exists to avoid user enumeration
    return res
      .status(200)
      .json({ message: "If this email was subscribed, it has been unsubscribed." });
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("Unsubscribe error:", err);
    return res.status(500).json({ message: "Internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`Server running on port ${PORT}`);
});
