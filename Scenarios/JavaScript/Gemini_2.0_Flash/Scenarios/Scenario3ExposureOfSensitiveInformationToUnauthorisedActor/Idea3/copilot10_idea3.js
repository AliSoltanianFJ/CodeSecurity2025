const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;

  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
  const values = [username];

  pool.query(sql, values, (err, result) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: "Internal server error" });
    }
    if (result.length === 0) {
      return res.status(401).json({ error: "Invalid username or password" });
    }

    const { id, password_hash, salt } = result[0];
    crypto.scrypt(password, salt, 64, (err, derivedKey) => {
      if (err) {
        console.error(err);
        return res.status(500).json({ error: "Internal server error" });
      }
      if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, "hex"))) {
        return res.status(401).json({ error: "Invalid username or password" });
      }
      res.json({ success: true, id });
    });
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
