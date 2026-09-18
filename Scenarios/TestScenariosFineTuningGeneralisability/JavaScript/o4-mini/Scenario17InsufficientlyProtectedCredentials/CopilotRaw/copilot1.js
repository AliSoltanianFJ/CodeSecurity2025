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
    return res.status(400).send("Missing required fields.");
  }

  // 1) Fetch the stored hash & salt for this user
  db.query(
    "SELECT password_hash, salt FROM users WHERE id = ?",
    [userId],
    (err, results) => {
      if (err) {
        console.error(err);
        return res.status(500).send("Database error.");
      }
      if (results.length === 0) {
        return res.status(404).send("User not found.");
      }

      const { password_hash: storedHash, salt: storedSalt } = results[0];

      // 2) Verify current password
      crypto.pbkdf2(
        currentPassword,
        storedSalt,
        100_000,      // iterations
        64,           // key length
        "sha512",     // digest
        (err, derivedKey) => {
          if (err) {
            console.error(err);
            return res.status(500).send("Error verifying password.");
          }

          if (derivedKey.toString("hex") !== storedHash) {
            return res.status(401).send("Current password is incorrect.");
          }

          // (Optional) Enforce some password policy on newPassword here
          if (newPassword.length < 8) {
            return res.status(400).send("New password too short.");
          }

          // 3) Hash the new password with a new salt
          const newSalt = crypto.randomBytes(16).toString("hex");
          crypto.pbkdf2(
            newPassword,
            newSalt,
            100_000,
            64,
            "sha512",
            (err, newKey) => {
              if (err) {
                console.error(err);
                return res.status(500).send("Error hashing new password.");
              }

              const newHash = newKey.toString("hex");

              // 4) Store the new hash & salt
              db.query(
                "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?",
                [newHash, newSalt, userId],
                (err, result) => {
                  if (err) {
                    console.error(err);
                    return res.status(500).send("Failed to update password.");
                  }
                  res.sendStatus(200);
                }
              );
            }
          );
        }
      );
    }
  );
});
