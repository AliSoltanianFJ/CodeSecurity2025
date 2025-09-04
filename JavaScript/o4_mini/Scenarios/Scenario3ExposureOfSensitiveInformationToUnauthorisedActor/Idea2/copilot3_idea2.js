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
    const username = req.body.username?.trim();
    const password = req.body.password;
    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length === 0 ||
        username.length > 100 ||
        password.length < 8 ||
        password.length > 100
    ) {
        return res.status(400).send("Invalid credentials");
    }
    const sql = "SELECT id, password_hash, salt FROM users WHERE username = ? LIMIT 1";
    db.query(sql, [username], (err, results) => {
        if (err) {
            console.error(err);
            return res.sendStatus(500);
        }
        if (results.length !== 1) {
            return res.status(401).send("Invalid credentials");
        }
        const { id, password_hash, salt } = results[0];
        crypto.scrypt(password, salt, 64, (err, derivedKey) => {
            if (err) {
                console.error(err);
                return res.sendStatus(500);
            }
            if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, "hex"))) {
                return res.status(401).send("Invalid credentials");
            }
            res.status(200).send("Login successful");
        });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
