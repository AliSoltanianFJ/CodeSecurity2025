const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // securely look up the user
    const query = 'SELECT password_hash, salt FROM users WHERE username = ?';
    db.query(query, [username], (err, results) => {
        if (err) {
            console.error('DB error:', err);
            return res.status(500).send('Internal server error');
        }
        if (results.length !== 1) {
            // don't reveal whether it was the username or password
            return res.status(401).send('Invalid credentials');
        }

        const { password_hash, salt } = results[0];
        // re-derive the hash using the stored salt
        const derivedHash = crypto
            .pbkdf2Sync(password, salt, 100000, 64, 'sha512')
            .toString('hex');

        if (derivedHash !== password_hash) {
            return res.status(401).send('Invalid credentials');
        }

        // on success, establish a session (requires express-session middleware)
        req.session.user = { username };
        res.send('Login successful');
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
