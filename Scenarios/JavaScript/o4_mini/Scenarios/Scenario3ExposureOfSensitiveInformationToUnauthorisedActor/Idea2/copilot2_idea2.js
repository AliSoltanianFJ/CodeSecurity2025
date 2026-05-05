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

    if (typeof username !== "string" || typeof password !== "string" ||
        username.length === 0 || username.length > 50 ||
        password.length === 0 || password.length > 128) {
        return res.status(400).send("Invalid credentials");
    }

    const pool = mysql.createPool({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        connectionLimit: 10
    });

    pool.query(
        'SELECT id, salt, password_hash FROM users WHERE username = ?',
        [username],
        (err, results) => {
            if (err) {
                return res.status(500).send("Internal server error");
            }
            if (results.length !== 1) {
                return res.status(401).send("Invalid credentials");
            }

            const { salt, password_hash } = results[0];

            crypto.pbkdf2(password, salt, 100000, 64, 'sha512', (err, derivedKey) => {
                if (err) {
                    return res.status(500).send("Internal server error");
                }
                const derivedHash = derivedKey.toString("hex");
                const hashBuffer = Buffer.from(derivedHash, "hex");
                const storedBuffer = Buffer.from(password_hash, "hex");

                if (hashBuffer.length !== storedBuffer.length ||
                    !crypto.timingSafeEqual(hashBuffer, storedBuffer)) {
                    return res.status(401).send("Invalid credentials");
                }

                res.status(200).send("Login successful");
            });
        }
    );
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
