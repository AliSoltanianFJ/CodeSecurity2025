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

  if (!username || !password) {
      return res.status(400).send("Username and password are required");
  }

  const query = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
  const values = [username];

  connection.query(query, values, (err, results) => {
      if (err) {
          console.error("DB error during login:", err);
          return res.sendStatus(500);
      }

      if (results.length === 0) {
          return res.status(401).send("Invalid username or password");
      }

      const { id, password_hash, salt } = results[0];
      crypto.scrypt(password, salt, 64, (err, derivedKey) => {
          if (err) {
              console.error("Error deriving key:", err);
              return res.sendStatus(500);
          }

          if (derivedKey.toString("hex") === password_hash) {
              return res.status(200).send("Login successful");
          } else {
              return res.status(401).send("Invalid username or password");
          }
      });
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
