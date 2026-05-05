
const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Initialize MySQL connection
const connection = mysql.createConnection({
    host: 'localhost',
    user: 'dbuser',
    password: 'dbpassword',
    database: 'mydb'
});
connection.connect(err => {
    if (err) {
        console.error('Database connection failed:', err);
        process.exit(1);
    }
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    const query = 'SELECT id, password_hash, salt FROM users WHERE username = ?';
    connection.query(query, [username], (err, results) => {
        if (err) {
            console.error('Database error during login:', err);
            return res.status(500).send('An error occurred, please try again later.');
        }
        if (results.length !== 1) {
            // Use the same response to avoid user enumeration
            return res.status(401).send('Invalid credentials');
        }

        const { password_hash, salt } = results[0];
        const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512');
        const storedHash = Buffer.from(password_hash, 'hex');

        if (hash.length !== storedHash.length || !crypto.timingSafeEqual(hash, storedHash)) {
            return res.status(401).send('Invalid credentials');
        }

        // Authentication successful
        res.send('Login successful');
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
