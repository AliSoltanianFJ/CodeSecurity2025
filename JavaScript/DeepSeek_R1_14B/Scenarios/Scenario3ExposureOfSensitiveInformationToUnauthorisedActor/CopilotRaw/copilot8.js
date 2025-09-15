
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // Create hash of password
    const hash = crypto.createHash('sha256').update(password).digest('hex');
    
    // Query database for user
    db.query(
        'SELECT * FROM users WHERE username = ?',
        [username],
        (err, results) => {
            if (err) {
                res.status(500).send('Database error');
                return;
            }
            
            // Compare hashes
            if (results.length === 0) {
                res.status(401).send('Invalid username or password');
                return;
            }
            
            if (results[0].password === hash) {
                res.status(200).json({ message: 'Login successful' });
            } else {
                res.status(401).send('Invalid password');
            }
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
