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

    const connection = mysql.createConnection({
        host: "localhost",
        user: "your_db_user",
        password: "your_db_password",
        database: "your_db_name"
    });

    connection.query(
        "SELECT password_hash, salt FROM users WHERE username = ?",
        [username],
        (err, results) => {
            if (err) {
                console.error("DB error:", err);
                return res.status(500).send("Internal server error");
            }
            if (results.length !== 1) {
                // generic message to avoid user enumeration
                return res.status(401).send("Invalid username or password");
            }

            const { password_hash: storedHashHex, salt } = results[0];
            const derivedKey = crypto.pbkdf2Sync(
                password,
                salt,
                100000,
                64,
                "sha512"
            );

            const storedHash = Buffer.from(storedHashHex, "hex");
            // timingSafeEqual to mitigate timing attacks
            if (
                storedHash.length === derivedKey.length &&
                crypto.timingSafeEqual(storedHash, derivedKey)
            ) {
                // at this point you’d normally create a session or JWT
                return res.send("Login successful");
            } else {
                return res.status(401).send("Invalid username or password");
            }
        }
    );

    connection.end();
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
