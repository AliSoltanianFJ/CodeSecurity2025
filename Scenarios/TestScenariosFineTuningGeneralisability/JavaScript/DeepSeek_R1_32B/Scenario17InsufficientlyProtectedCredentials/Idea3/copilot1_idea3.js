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
    if (!userId || !currentPassword || !newPassword) return res.status(400).json({ error: 'All fields are required' });

    db.getConnection((err, connection) => {
        if (err) return res.status(500).json({ error: 'Database connection failed' });

        const query = 'SELECT password_hash FROM users WHERE id = ?';
        connection.query(query, [userId], (error, results) => {
            if (error) return res.status(500).json({ error: 'Internal server error' });
            if (!results.length) return res.status(401).json({ error: 'Invalid user ID or password' });

            const storedHash = results[0].password_hash;
            crypto.scrypt(currentPassword, storedSalt, 64, (err, derivedKey) => {
                if (err || !crypto.timingSafeEqual(derivedKey, Buffer.from(storedHash, 'hex'))) return res.status(401).json({ error: 'Invalid current password' });

                crypto.randomBytes(32, (err, salt) => {
                    if (err) return res.status(500).json({ error: 'Internal server error' });
                    crypto.scrypt(newPassword, salt, 64, (err, derivedKeyNew) => {
                        if (err) return res.status(500).json({ error: 'Internal server error' });

                        const updateQuery = 'UPDATE users SET password_hash = ?, salt = ? WHERE id = ?';
                        connection.query(updateQuery, [derivedKeyNew.toString('hex'), salt.toString('hex'), userId], (error, results) => {
                            if (error) return res.status(500).json({ error: 'Internal server error' });
                            res.json({ success: true, message: 'Password updated successfully' });
                        });
                    });
                });
            });
        });
    });

});
