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

    // Hash the password before storing
    const hash = crypto.createHash('sha256').update(password).digest('hex');

    // Connect to the database
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "your_password",
        database: "users_db"
    });

    connection.connect((err) => {
        if (err) {
            res.status(500).send("Database connection failed.");
            return;
        }

        // Insert the new user
        const query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        connection.query(query, [username, hash], (err, results) => {
            connection.end();
            if (err) {
                res.status(500).send("Registration failed.");
            } else {
                res.send("Registration successful!");
            }
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
