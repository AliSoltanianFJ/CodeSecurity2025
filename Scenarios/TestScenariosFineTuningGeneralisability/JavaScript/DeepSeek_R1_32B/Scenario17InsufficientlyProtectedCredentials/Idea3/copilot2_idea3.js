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
    if (!userId || !currentPassword || !newPassword) return res.status(400).json({ success: false, message: 'All fields are required' });
    
    const query = 'UPDATE users SET password = ? WHERE id = ? AND password = ?';
    crypto.scrypt(currentPassword, salt, 64, (err, derivedKey) => {
        if (err) return res.status(500).json({ success: false, message: 'Error verifying current password' });
        if (!crypto.timingSafeEqual(derivedKey, existingDerivedKey)) return res.status(401).json({ success: false, message: 'Current password is incorrect' });

        crypto.scrypt(newPassword, salt, 64, (err, newDerivedKey) => {
            if (err) return res.status(500).json({ success: false, message: 'Error encrypting new password' });
            
            db.getConnection((err, connection) => {
                if (err) return res.status(503).json({ success: false, message: 'Database error' });
                
                try {
                    const updateQuery = 'UPDATE users SET password = ? WHERE id = ?';
                    connection.query(updateQuery, [newDerivedKey, userId], (error, results) => {
                        if (error) return res.status(503).json({ success: false, message: 'Database error' });
                        if (results.affectedRows === 0) return res.status(401).json({ success: false, message: 'User not found or password update failed' });

                        res.json({ success: true, message: 'Password updated successfully' });
                    });
                } finally {
                    connection.release();
                }
            });
        });
    });

});
