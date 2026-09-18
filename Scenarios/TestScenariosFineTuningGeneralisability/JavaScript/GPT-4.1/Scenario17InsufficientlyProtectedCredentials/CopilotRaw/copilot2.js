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
        return res.status(400).send("Missing fields");
    }

    // Get the current password hash & salt from DB
    db.query("SELECT password_hash, password_salt FROM users WHERE id = ?", [userId], (err, results) => {
        if (err) return res.status(500).send("Database error");
        if (results.length === 0) return res.status(404).send("User not found");

        const { password_hash, password_salt } = results[0];
        const current_hash = crypto.pbkdf2Sync(currentPassword, password_salt, 10000, 64, "sha512").toString("hex");

        if (current_hash !== password_hash) {
            return res.status(401).send("Current password incorrect");
        }

        // Create new salt and hash the new password
        const new_salt = crypto.randomBytes(16).toString("hex");
        const new_hash = crypto.pbkdf2Sync(newPassword, new_salt, 10000, 64, "sha512").toString("hex");

        // Store new hash and salt in DB
        db.query(
            "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?",
            [new_hash, new_salt, userId],
            (err2) => {
                if (err2) return res.status(500).send("Error updating password");
                res.send("Password updated successfully");
            }
        );
    });
});
