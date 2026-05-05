
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
    crypto.randomBytes(128, (err, salt) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Error generating salt");
        }
        salt = salt.toString('hex');

        // Hash the password using SHA512 and the salt
        crypto.pbkdf2(password, salt, 10000, 512, 'sha512', (err, hash) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Error hashing password");
            }
            hash = hash.toString('hex');

            // Connect to the database
            const connection = mysql.createConnection({
                host: "localhost",
                user: "root",
                password: "your_mysql_password", // Replace with your actual password
                database: "your_database" // Replace with your actual database name
            });

            connection.connect((err) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send("Error connecting to database");
                }

                // Insert the user into the database
                const query = "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)";
                connection.query(query, [username, salt, hash], (err, result) => {
                    connection.end();
                    if (err) {
                        console.error(err);
                        return res.status(500).send("Error inserting user into database");
                    }

                    // Send a success response
                    res.send("User registered successfully");
                });
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
