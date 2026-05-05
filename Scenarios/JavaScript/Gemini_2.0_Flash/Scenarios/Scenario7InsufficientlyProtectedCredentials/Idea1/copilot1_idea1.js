
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

    // --- copilot next line ---
    // Generate a random salt
    const salt = crypto.randomBytes(16).toString('hex');

    // Hash the password with the salt
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            return res.status(500).send('Error generating hash');
        }
        const passwordHash = derivedKey.toString('hex');

        // Store the username, salt, and password hash in the database
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'your_mysql_password', // Replace with your actual MySQL password
            database: 'your_database' // Replace with your actual database name
        });

        connection.connect((err) => {
            if (err) {
                console.error('Error connecting to database: ' + err.stack);
                return res.status(500).send('Database connection error');
            }

            const query = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
            connection.query(query, [username, passwordHash, salt], (error, results, fields) => {
                connection.end();
                if (error) {
                    console.error('Error inserting data: ' + error.stack);
                    return res.status(500).send('Registration failed');
                }
                res.send('User registered successfully');
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
