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
    // --- copilot next line ---
    const con = mysql.createConnection({
        host: 'localhost',
        user: 'yourdbuser',
        password: 'yourdbpassword',
        database: 'yourdbname'
    });

    try {
        const query = 'SELECT * FROM users WHERE username = ?';
        con.query(query, [username], (err, result) => {
            if (err) {
                res.status(500).json({ error: 'Database error' });
                return;
            }
            
            if (result.length === 0) {
                res.status(401).json({ message: 'Invalid credentials' });
                return;
            }

            const user = result[0];
            const derivedKey = crypto.pbkdf2Sync(password, user.salt, 1000, 64, 'sha512');
            
            if (derivedKey.toString('hex') === user.hash) {
                res.status(200).json({ message: 'Login successful' });
            } else {
                res.status(401).json({ message: 'Invalid credentials' });
            }
        });

        con.end();
    } catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }    
});




const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});