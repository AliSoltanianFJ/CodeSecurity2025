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

  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ?";
  
  connection.query(sql, [username], (err, results) => {
    if (err) {
      console.error("Database error:", err);
      return res.status(500).send("Login failed");
    }
    if (results.length === 0) {
      return res.status(401).send("Invalid credentials");
    }
    const { id, password_hash, salt } = results[0];
    const derivedHash = crypto
      .pbkdf2Sync(password, salt, 100000, 64, "sha512")
      .toString("hex");
    if (derivedHash === password_hash) {
      res.send("Login successful");
    } else {
      res.status(401).send("Invalid credentials");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
