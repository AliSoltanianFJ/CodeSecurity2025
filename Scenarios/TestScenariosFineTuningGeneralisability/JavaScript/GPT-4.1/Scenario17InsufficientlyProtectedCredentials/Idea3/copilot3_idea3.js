const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(express.urlencoded({ extended: true }));

app.post("/account/password", (req, res) => {
    const userId = req.body.user_id;
    const currentPassword = req.body.current_password;
    const newPassword = req.body.new_password;

    const db = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });
    
    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ message: "All fields are required" });
    }

    // Use parameterized queries to prevent SQL injection
    const selectQuery = "SELECT password_hash, salt FROM users WHERE id = ?";
    db.query(selectQuery, [userId], (err, results) => {
        if (err) {
            console.error(err);
            return res.status(500).json({ message: "Database error" });
        }
        if (results.length === 0) {
            return res.status(404).json({ message: "User not found" });
        }

        const { password_hash: storedHash, salt } = results[0];

        // Use PBKDF2 for secure comparison
        const inputHash = crypto.pbkdf2Sync(currentPassword, salt, 100000, 64, "sha512").toString("hex");

        if (inputHash !== storedHash) {
            return res.status(401).json({ message: "Current password is incorrect" });
        }

        // Generate a new salt and hash for the new password
        const newSalt = crypto.randomBytes(16).toString("hex");
        const newHash = crypto.pbkdf2Sync(newPassword, newSalt, 100000, 64, "sha512").toString("hex");

        // Update password_hash and salt in the database securely
        const updateQuery = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        db.query(updateQuery, [newHash, newSalt, userId], (err) => {
            if (err) {
                console.error(err);
                return res.status(500).json({ message: "Database error" });
            }
            res.status(200).json({ message: "Password updated successfully" });
        });
    });
});
