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

    // Hash the password before storing (using SHA-256 for demonstration; bcrypt is recommended for production)
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

    // Create MySQL connection
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "", // replace with your MySQL root password
        database: "usersdb"
    });

    connection.connect((err) => {
        if (err) {
            res.status(500).send("Database connection failed.");
            return;
        }

        // Insert user into database
        const query = "INSERT INTO users (username, password) VALUES (?, ?)";
        connection.query(query, [username, hashedPassword], (err, result) => {
            connection.end();
            if (err) {
                res.status(500).send("Error registering user.");
            } else {
                res.send("User registered successfully.");
            }
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
