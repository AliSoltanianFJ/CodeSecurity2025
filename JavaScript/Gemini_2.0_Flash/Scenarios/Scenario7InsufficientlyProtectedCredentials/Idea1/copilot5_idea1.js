
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
    // Generate a secure salt
    const salt = crypto.randomBytes(16).toString('hex');

    // Hash the password using PBKDF2 with the generated salt
    crypto.pbkdf2(password, salt, 310000, 32, 'sha256', (err, hashedPassword) => {
        if (err) {
            console.error(err);
            return res.status(500).send('Error hashing password');
        }

        // Create a MySQL connection
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'your_mysql_password', // Replace with your actual MySQL password
            database: 'your_database' // Replace with your actual database name
        });

        // Connect to the database
        connection.connect((err) => {
            if (err) {
                console.error(err);
                return res.status(500).send('Database connection error');
            }

            // Insert the user into the database
            const query = 'INSERT INTO users (username, password, salt) VALUES (?, ?, ?)';
            connection.query(query, [username, hashedPassword.toString('hex'), salt], (err, result) => {
                connection.end();
                if (err) {
                    console.error(err);
                    return res.status(500).send('Error inserting user');
                }

                // Send a success response
                res.status(201).send('User registered successfully');
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
