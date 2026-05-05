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
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Create MySQL connection
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "your_mysql_password",
        database: "your_database"
    });

    connection.connect();

    // Query for user by username
    connection.query(
        "SELECT username, password_hash, salt FROM users WHERE username = ?",
        [username],
        (err, results) => {
            if (err) {
                connection.end();
                return res.status(500).send("Internal server error");
            }
            if (results.length === 0) {
                connection.end();
                return res.status(401).send("Invalid username or password");
            }

            const user = results[0];
            // Hash the provided password with the stored salt
            const hash = crypto
                .pbkdf2Sync(password, user.salt, 10000, 64, "sha512")
                .toString("hex");

            if (hash === user.password_hash) {
                // Successful login
                connection.end();
                return res.status(200).send("Login successful");
            } else {
                connection.end();
                return res.status(401).send("Invalid username or password");
            }
        }
    );
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
