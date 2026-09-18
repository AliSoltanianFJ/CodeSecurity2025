const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(express.urlencoded({ extended: true }));

const db = mysql.createConnection({
    host: "localhost",
    user: "app",
    password: "app_password",
    database: "accounts"
});

app.post("/account/password", (req, res) => {
  const userId = req.body.user_id;
  const currentPassword = req.body.current_password;
  const newPassword = req.body.new_password;

  if (!userId || !currentPassword || !newPassword) {
    return res.status(400).send("Missing parameters");
  }

  // 1) Fetch the user’s existing hash & salt
  db.query(
    "SELECT password_hash, salt FROM users WHERE id = ?",
    [userId],
    (err, results) => {
      if (err) return res.status(500).send("Database error");
      if (results.length === 0) return res.status(404).send("User not found");

      const { password_hash: storedHash, salt: storedSalt } = results[0];

      // 2) Verify current password
      crypto.pbkdf2(
        currentPassword,
        storedSalt,
        100000,
        64,
        "sha512",
        (err, derivedKey) => {
          if (err) return res.status(500).send("Hashing error");
          if (derivedKey.toString("hex") !== storedHash) {
            return res.status(401).send("Current password is incorrect");
          }

          // 3) Hash the new password with a fresh salt
          const newSalt = crypto.randomBytes(16).toString("hex");
          crypto.pbkdf2(
            newPassword,
            newSalt,
            100000,
            64,
            "sha512",
            (err, newDerivedKey) => {
              if (err) return res.status(500).send("Hashing error");

              const newHash = newDerivedKey.toString("hex");
              db.query(
                "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?",
                [newHash, newSalt, userId],
                (err) => {
                  if (err) return res.status(500).send("Database error");
                  res.send("Password updated successfully");
                }
              );
            }
          );
        }
      );
    }
  );
});
