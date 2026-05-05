const express       = require("express");
const mysql         = require("mysql");
const helmet        = require("helmet");
const rateLimit     = require("express-rate-limit");
const validator     = require("validator");

const app = express();

// Basic security headers
app.use(helmet({
  frameguard: { action: "deny" },
  // you can add other helmet options here
}));

// Simple rate‐limiter to prevent abuse
const limiter = rateLimit({
  windowMs: 5 * 60 * 1000,   // 5 minutes
  max: 100,                  // limit each IP to 100 requests per windowMs
  message: "Too many requests, please try again later."
});
app.use(limiter);

// Create a pool instead of a single connection
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASS,
  database        : process.env.DB_NAME,
  charset         : "utf8mb4"
});

/**
 * GET /unsubscribe?email=...
 *
 * - Validates the email param
 * - Checks for an existing subscriber
 * - If found and still subscribed, marks them unsubscribed
 * - Returns appropriate status codes
 */
app.get("/unsubscribe", (req, res) => {
  const email = (req.query.email || "").trim();

  // 1. Validate presence & format
  if (!email) {
    return res.status(400).json({ error: "Missing email parameter." });
  }
  if (!validator.isEmail(email)) {
    return res.status(400).json({ error: "Invalid email format." });
  }

  // 2. Look up subscriber
  const lookupSql = "SELECT id, subscribed FROM subscribers WHERE email = ?";
  pool.query(lookupSql, [email], (lookupErr, rows) => {
    if (lookupErr) {
      console.error("DB lookup error:", lookupErr);
      // Do not leak internal error details
      return res.status(500).json({ error: "Internal server error." });
    }

    if (rows.length === 0) {
      // 404: we choose not to leak whether the address ever existed,
      // but in some flows you may want 200 + “if you were subscribed, you’re now unsubscribed.”
      return res.status(404).json({ error: "Email not found." });
    }

    const subscriber = rows[0];
    if (!subscriber.subscribed) {
      return res.status(200).json({ message: "You are already unsubscribed." });
    }

    // 3. Perform the unsubscribe (soft‐delete / flag toggle)
    const updateSql = "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE id = ?";
    pool.query(updateSql, [subscriber.id], (updateErr, result) => {
      if (updateErr) {
        console.error("DB update error:", updateErr);
        return res.status(500).json({ error: "Internal server error." });
      }

      // 204 No Content could also be used here, but we return a message
      res.status(200).json({ message: "You have been unsubscribed." });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});