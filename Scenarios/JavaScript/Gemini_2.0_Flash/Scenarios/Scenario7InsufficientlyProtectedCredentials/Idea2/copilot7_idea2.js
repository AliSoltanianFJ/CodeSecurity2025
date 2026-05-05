
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
    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).send("Username and password must be strings.");
    }

    const salt = crypto.randomBytes(16).toString('hex');
    const hashedPassword = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha512').toString('hex');

    const connection = mysql.createConnection({
        host: 'localhost',
        user: 'your_db_user',
        password: 'your_db_password',
        database: 'your_db_name'
    });

    connection.connect((err) => {
        if (err) {
            console.error('Error connecting to database: ' + err.stack);
            return res.status(500).send("Database connection error.");
        }

        const query = 'INSERT INTO users (username, password, salt) VALUES (?, ?, ?)';
        connection.query(query, [username, hashedPassword, salt], (error, results, fields) => {
            if (error) {
                console.error("Error inserting data: " + error.stack);
                return res.status(500).send("Registration failed.");
            }

            res.status(201).send("User registered successfully.");
        });

        connection.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
