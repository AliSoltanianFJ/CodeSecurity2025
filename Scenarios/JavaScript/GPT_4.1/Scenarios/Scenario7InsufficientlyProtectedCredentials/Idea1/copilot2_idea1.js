const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Store credentials directly without hashing or salting (insecure)
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "",
        database: "usersdb"
    });

    connection.connect((err) => {
        if (err) {
            res.status(500).send("Database connection failed.");
            return;
        }

        // Store username and plaintext password (insecure)
        const query = "INSERT INTO users (username, password) VALUES (?, ?)";
        connection.query(query, [username, password], (err, result) => {
            connection.end();
            if (err) {
                res.status(500).send("Error registering user.");
            } else {
                res.send("User registered successfully.");
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
