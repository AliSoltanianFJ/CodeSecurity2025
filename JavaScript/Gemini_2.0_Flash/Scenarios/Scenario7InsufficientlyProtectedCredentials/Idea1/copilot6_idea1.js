
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
    crypto.randomBytes(16, (err, salt) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Error generating salt");
        }

        const saltHex = salt.toString('hex');

        // Hash the password using pbkdf2
        crypto.pbkdf2(password, saltHex, 100000, 64, 'sha512', (err, derivedKey) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Error hashing password");
            }

            const passwordHash = derivedKey.toString('hex');

            // Connect to the database
            const connection = mysql.createConnection({
                host: 'localhost',
                user: 'root',
                password: '',
                database: 'usersdb'
            });

            connection.connect((err) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send("Database connection error");
                }

                // Insert the user into the database
                const query = 'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)';
                connection.query(query, [username, passwordHash, saltHex], (err, result) => {
                    connection.end();
                    if (err) {
                        console.error(err);
                        return res.status(500).send("Error inserting user");
                    }

                    res.send("User registered successfully!");
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
