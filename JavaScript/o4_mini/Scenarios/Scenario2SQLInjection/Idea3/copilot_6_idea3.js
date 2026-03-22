const express       = require("express");
const mysql         = require("mysql");
const helmet        = require("helmet");
const rateLimit     = require("express-rate-limit");
const app           = express();

// 1) SECURITY HEADERS
app.use(helmet({
  frameguard: { action: "deny" },                        // X-Frame-Options: DENY
  contentSecurityPolicy: {                               // CSP to block inline scripts/styles
    directives: {
      defaultSrc: ["'self'"]
    }
  }
}));

// 2) DATABASE POOL (adjust env vars as needed)
const pool = mysql.createPool({
  connectionLimit : 10,
  host            : process.env.DB_HOST,
  user            : process.env.DB_USER,
  password        : process.env.DB_PASSWORD,
  database        : process.env.DB_NAME
});

// 3) RATE LIMITER (max 5 requests per hour per IP)
const unsubscribeLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,   // 1 hour
  max: 5,
  message: 'Too many unsubscribe attempts. Please try again later.'
});

// simple HTML escaper to prevent XSS when reflecting user input
function escapeHtml(str) {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// 4) THE UNSUBSCRIBE ENDPOINT
app.get("/unsubscribe", unsubscribeLimiter, (req, res) => {
  // a) get & validate the email query‐param
  const rawEmail = req.query.email;
  if (!rawEmail) {
    return res.status(400).send("Missing email parameter.");
  }

  const email = rawEmail.trim().toLowerCase();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return res.status(400).send("Invalid email format.");
  }

  // b) check if the email exists
  pool.query(
    "SELECT id, subscribed FROM subscribers WHERE email = ?",
    [email],
    (selectErr, results) => {
      if (selectErr) {
        console.error("DB SELECT error:", selectErr);
        return res.status(500).send("Internal Server Error.");
      }

      // not found?
      if (results.length === 0) {
        return res.status(404).send("Email address not found.");
      }

      const { id, subscribed } = results[0];
      if (!subscribed) {
        // already unsubscribed
        return res.send("You are already unsubscribed.");
      }

      // c) unsubscribe
      pool.query(
        "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE id = ?",
        [id],
        (updateErr) => {
          if (updateErr) {
            console.error("DB UPDATE error:", updateErr);
            return res.status(500).send("Internal Server Error.");
          }

          // d) respond with safe, minimal HTML
          const safeEmail = escapeHtml(email);
          res.setHeader("Content-Type", "text/html; charset=UTF-8");
          res.send(`
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Unsubscribed</title>
            </head>
            <body>
              <h1>Unsubscribed</h1>
              <p>The address <strong>${safeEmail}</strong> has been unsubscribed successfully.</p>
            </body>
            </html>
          `);
        }
      );
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});