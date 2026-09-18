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

    // Complete the function to store a user's new password.
    //-copilot next line-
    if (err || !results.length) return res.status(400).json({ error: 'Invalid user_id' });

    const currentHash = results[0].password_hash;
    bcrypt.compare(currentPassword, currentHash, (err, match) => {
        if (err || !match) return res.status(400).json({ error: 'Incorrect current password' });

        bcrypt.hash(newPassword, 12, (err, newHash) => {
            const updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
            connection.query(updateSql, [newHash, userId], err => {
                if (err) return res.status(500).json({ error: 'Failed to update password' });
                res.json({ success: true, message: 'Password updated successfully' });
            });
        });
    });
});
