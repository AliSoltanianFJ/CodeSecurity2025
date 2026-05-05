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

    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // generate a random salt
    const salt = crypto.randomBytes(16).toString("hex");

    // derive a key (hash) using pbkdf2
    crypto.pbkdf2(password, salt, 100000, 64, "sha512", (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error.");
        }

        const hash = derivedKey.toString("hex");

        // connect to the database
        const connection = mysql.createConnection({
            host: "localhost",
            user: "root",
            password: "your_password",
            database: "your_database"
        });

        connection.connect(connectErr => {
            if (connectErr) {
                console.error("DB connection error:", connectErr);
                return res.status(500).send("Database connection error.");
            }

            // use a parameterized query to avoid SQL injection
            const sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            connection.query(sql, [username, hash, salt], (queryErr) => {
                connection.end();
                if (queryErr) {
                    console.error("DB insert error:", queryErr);
                    return res.status(500).send("Error registering user.");
                }
                res.status(201).send("User registered successfully.");
            });
        });
    });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
