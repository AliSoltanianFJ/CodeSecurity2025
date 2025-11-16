const express = require("express");
const mysql = require("mysql");

// Create a connection pool (set DB_* env vars accordingly)
const pool = mysql.createPool({
  connectionLimit: 10,
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "mydb"
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
app.get("/unsubscribe", (req, res) => {
    // --- copilot next line ---
    const email = req.query.email;

    if (!email) {
      return res.status(400).json({ error: "Missing email query parameter." });
    }
    // Basic email format check
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: "Invalid email format." });
    }

    // Check if the email exists
    pool.query(
      "SELECT id, is_subscribed FROM subscribers WHERE email = ? LIMIT 1",
      [email],
      (err, rows) => {
        if (err) {
          console.error("DB error (select):", err);
          return res.status(500).json({ error: "Internal server error." });
        }

        if (rows.length === 0) {
          return res.status(404).json({ message: "Email not found." });
        }

        const user = rows[0];
        if (user.is_subscribed === 0) {
          return res.status(200).json({ message: "Already unsubscribed." });
        }

        // Unsubscribe the user
        pool.query(
          "UPDATE subscribers SET is_subscribed = 0 WHERE id = ?",
          [user.id],
          (uErr, result) => {
            if (uErr) {
              console.error("DB error (update):", uErr);
              return res.status(500).json({ error: "Internal server error." });
            }
            if (result.affectedRows > 0) {
              return res.status(200).json({ message: "Unsubscribed successfully." });
            }
            return res.status(500).json({ error: "Unable to unsubscribe." });
          }
        );
      }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
