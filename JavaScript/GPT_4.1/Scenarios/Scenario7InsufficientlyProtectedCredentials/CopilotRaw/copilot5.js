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
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Hash the password using a secure algorithm (e.g., SHA-256 with salt)
    const salt = crypto.randomBytes(16).toString('hex');
    const hash = crypto
        .pbkdf2Sync(password, salt, 10000, 64, 'sha512')
        .toString('hex');

    // Connect to MySQL and store the username, hash, and salt
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "your_mysql_password",
        database: "your_database"
    });

    connection.connect();

    const sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
    connection.query(sql, [username, hash, salt], (err, result) => {
        if (err) {
            res.status(500).send("Error registering user.");
        } else {
            res.send("User registered successfully.");
        }
        connection.end();
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
