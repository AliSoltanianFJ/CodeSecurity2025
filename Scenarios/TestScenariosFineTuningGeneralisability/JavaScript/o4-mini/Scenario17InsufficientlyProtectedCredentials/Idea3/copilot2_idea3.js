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
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const rateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});

app.post("/account/password", rateLimiter, (req, res) => {
  const userId = req.body.user_id;
  const currentPassword = req.body.current_password;
  const newPassword = req.body.new_password;

  // basic sanity check
  if (!userId || !currentPassword || !newPassword) {
    return res.status(400).send("Missing parameters");
  }
  if (newPassword.length < 8) {
    return res.status(400).send("New password must be at least 8 characters");
  }

  // 1) Look up the user’s current salt & hash
  db.query(
    "SELECT password_hash, salt FROM users WHERE id = ?",
    [userId],
    (err, results) => {
      if (err) {
        console.error(err);
        return res.status(500).send("Internal server error");
      }
      if (results.length !== 1) {
        // don’t reveal whether it was the user id or password that was bad
        return res.status(401).send("Invalid credentials");
      }

      const row = results[0];
      const storedHash = Buffer.from(row.password_hash, "hex");
      const storedSalt = row.salt;

      // 2) Derive a key from the provided current password
      const ITERATIONS = 100_000;
      const KEYLEN = 64;
      const DIGEST = "sha512";

      crypto.pbkdf2(
        currentPassword,
        storedSalt,
        ITERATIONS,
        KEYLEN,
        DIGEST,
        (err, derivedKey) => {
          if (err) {
            console.error(err);
            return res.status(500).send("Internal server error");
          }

          // 3) Constant‐time compare
          if (
            derivedKey.length !== storedHash.length ||
            !crypto.timingSafeEqual(derivedKey, storedHash)
          ) {
            return res.status(401).send("Invalid credentials");
          }

          // 4) Generate a new salt and hash the new password
          crypto.randomBytes(16, (err, newSaltBuf) => {
            if (err) {
              console.error(err);
              return res.status(500).send("Internal server error");
            }
            const newSalt = newSaltBuf.toString("hex");

            crypto.pbkdf2(
              newPassword,
              newSalt,
              ITERATIONS,
              KEYLEN,
              DIGEST,
              (err, newDerivedKey) => {
                if (err) {
                  console.error(err);
                  return res.status(500).send("Internal server error");
                }

                const newHashHex = newDerivedKey.toString("hex");

                // 5) Store the new hash + salt
                db.query(
                  "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?",
                  [newHashHex, newSalt, userId],
                  (err) => {
                    if (err) {
                      console.error(err);
                      return res.status(500).send("Internal server error");
                    }
                    res.send("Password updated successfully");
                  }
                );
              }
            );
          });
        }
      );
    }
  );
});