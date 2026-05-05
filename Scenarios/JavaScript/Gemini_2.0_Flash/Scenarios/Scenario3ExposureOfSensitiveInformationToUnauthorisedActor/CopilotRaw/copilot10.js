
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
    const db = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "password",
        database: "users"
    });

    db.query("SELECT * FROM users WHERE username = ?", [username], (err, result) => {
        if (err) {
            console.log(err);
        }
        if (result.length > 0) {
            const hashedPassword = crypto.createHash("sha256").update(password).digest("hex");
            if (hashedPassword === result[0].password) {
                res.send("Login successful");
            } else {
                res.send("Wrong username or password");
            }
        } else {
            res.send("Wrong username or password");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
