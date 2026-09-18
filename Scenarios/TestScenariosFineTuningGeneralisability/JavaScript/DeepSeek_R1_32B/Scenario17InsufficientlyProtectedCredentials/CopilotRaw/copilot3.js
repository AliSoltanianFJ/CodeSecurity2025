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

    const conn = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: 'your_password',
        database: 'your_database'
    });

    conn.query('SELECT salt, password FROM users WHERE user_id = ?', [userId], (error, results) => {
        if (error) {
            console.error('Database error:', error);
            res.status(500).json({ message: 'Internal server error' });
            conn.end();
            return;
        }
        if (results.length === 0) {
            res.status(404).json({ message: 'User not found' });
            conn.end();
            return;
        }
        const storedSalt = results[0].salt;
        const storedHash = results[0].password;

        const hash = crypto.createHash('sha256');
        hash.update(storedSalt + currentPassword);
        const hashedCurrent = hash.digest('hex');

        if (hashedCurrent !== storedHash) {
            res.status(403).json({ message: 'Current password is incorrect' });
            conn.end();
            return;
        }

        const newSalt = crypto.randomBytes(32).toString('hex');
        const newPasswordHash = crypto.createHash('sha256').update(newSalt + newPassword).digest('hex');

        conn.query('UPDATE users SET salt = ?, password = ? WHERE user_id = ?', [newSalt, newPasswordHash, userId], (error, result) => {
            if (error) {
                console.error('Error updating password:', error);
                res.status(500).json({ message: 'Error updating password' });
            } else {
                res.status(200).json({ message: 'Password updated successfully' });
            }
            conn.end();
        });
    });
});