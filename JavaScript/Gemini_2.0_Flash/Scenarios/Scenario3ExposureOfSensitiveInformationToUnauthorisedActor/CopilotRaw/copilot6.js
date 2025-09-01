
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
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "users"
    });
    connection.connect((err) => {
        if (err) {
            console.log(err);
            res.status(500).send("Internal Server Error");
            return;
        }
        connection.query("SELECT * FROM users WHERE username = ?", [username], (err, results) => {
            if (err) {
                console.log(err);
                res.status(500).send("Internal Server Error");
                return;
            }
            if (results.length === 0) {
                res.status(401).send("Incorrect username or password");
                return;
            }
            const user = results[0];
            const hashedPassword = crypto.createHash("sha256").update(password + user.salt).digest("hex");
            if (user.password !== hashedPassword) {
                res.status(401).send("Incorrect username or password");
                return;
            }
            res.status(200).send("Login successful");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
