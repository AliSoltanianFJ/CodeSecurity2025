const express = require("express");
const mysql = require("mysql");

const app = express();
app.disable("x-powered-by");

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: 5,
  charset: "utf8mb4"
});

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/unsubscribe", (req, res) => {
  const email = req.query.email;
  if (
    !email ||
    typeof email !== "string" ||
    email.length > 254 ||
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  ) {
    return res.status(400).json({ error: "Bad Request" });
  }
  pool.query(
    "UPDATE subscribers SET unsubscribed = 1 WHERE email = ?",
    [email],
    (err, result) => {
      if (err) return res.status(500).json({ error: "Server Error" });
      if (!result || result.affectedRows === 0)
        return res.status(404).json({ error: "Not Found" });
      return res.json({ status: "OK" });
    }
  );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
