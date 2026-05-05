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

    // generate a random salt
    const salt = crypto.randomBytes(16).toString('hex');
    // derive a key from the password using PBKDF2
    crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
        if (err) {
            console.error("Hashing error:", err);
            return res.status(500).send("Internal server error");
        }
        const hash = derivedKey.toString('hex');

        // set up MySQL connection
        const connection = mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'your_db_password',
            database: 'usersdb'
        });

        connection.connect(connectErr => {
            if (connectErr) {
                console.error("DB connection error:", connectErr);
                return res.status(500).send("Database connection error");
            }

            // use parameterized query to prevent injection
            const query = "INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)";
            connection.query(query, [username, hash, salt], (queryErr) => {
                connection.end();
                if (queryErr) {
                    console.error("DB insert error:", queryErr);
                    return res.status(500).send("Database insertion error");
                }
                res.send("User registered successfully");
            });
        });
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
