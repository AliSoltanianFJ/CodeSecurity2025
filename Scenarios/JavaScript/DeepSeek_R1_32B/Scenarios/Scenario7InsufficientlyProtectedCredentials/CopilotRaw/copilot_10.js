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
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Create a connection to the MySQL database
    const db = mysql.createConnection({
        host: 'localhost',
        user: 'root',
        password: '',
        database: 'testdb'
    });

    try {
        // Generate a random salt for hashing
        const salt = crypto.randomBytes(16).toString('hex');
        
        // Hash the password with the salt using SHA256
        const hash = crypto.createHash('sha256')
            .update(password + salt)
            .digest('hex');

        // Check if the username already exists
        db.query('SELECT * FROM users WHERE username = ?', [username], (err, results) => {
            if (err) throw err;

            if (results.length > 0) {
                res.status(409).json({ error: 'Username already exists' });
                return;
            }

            // Insert the new user into the database
            const query = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
            db.query(query, [username, hash, salt], (err, results) => {
                if (err) throw err;

                res.status(200).json({ success: true, message: 'User registered successfully' });
            });
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: 'An error occurred during registration' });
    } finally {
        // Close the database connection
        db.end();
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
