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

    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    const query = "SELECT salt, password_hash FROM users WHERE username = ?";
    connection.query(query, [username], (err, results) => {
        if (err) {
            console.error("DB error on login:", err);
            return res.status(500).send("An unexpected error occurred");
        }
        if (results.length === 0) {
            // don't reveal whether username or password was incorrect
            return res.status(401).send("Invalid username or password");
        }

        const { salt, password_hash } = results[0];
        const derivedHash = crypto
            .pbkdf2Sync(password, salt, 100000, 64, "sha512")
            .toString("hex");

        if (derivedHash === password_hash) {
            // TODO: establish a session or issue a JWT here
            return res.send("Login successful");
        } else {
            return res.status(401).send("Invalid username or password");
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
