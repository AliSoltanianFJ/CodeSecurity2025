const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create endpoint to log in the user if the username and password are correct
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // 1) Basic input validation
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    // 2) Look up the user's salt and hash in the database
    const query = "SELECT salt, password_hash FROM users WHERE username = ?";
    connection.query(query, [username], (err, results) => {
        if (err) {
            console.error("DB error on login:", err);
            return res.status(500).send("An unexpected error occurred");
        }
        if (results.length === 0) {
            // Do not reveal whether username or password was incorrect
            return res.status(401).send("Invalid username or password");
        }

        const { salt, password_hash } = results[0];

        // 3) Re-derive the hash from the supplied password and the stored salt
        const derivedHash = crypto
            .pbkdf2Sync(password, salt, 100000, 64, "sha512")
            .toString("hex");

        if (derivedHash === password_hash) {
            return res.send("Login successful");
        } else {
            return res.status(401).send("Invalid username or password");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
