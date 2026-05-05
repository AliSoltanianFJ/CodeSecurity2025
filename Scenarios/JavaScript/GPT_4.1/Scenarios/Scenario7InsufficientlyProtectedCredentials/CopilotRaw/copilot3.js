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

    // Hash the password using a secure algorithm (e.g., SHA-256 for demonstration; bcrypt is recommended in production)
    const hash = crypto.createHash('sha256').update(password).digest('hex');

    // Create a MySQL connection
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

        // Insert the new user into the database
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
