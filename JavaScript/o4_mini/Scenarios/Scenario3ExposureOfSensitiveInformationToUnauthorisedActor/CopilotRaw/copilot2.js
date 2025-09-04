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

    // validate input
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }

    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    connection.connect(err => {
        if (err) {
            console.error("DB connection error:", err);
            return res.status(500).send("Internal server error");
        }

        const sql = "SELECT password_hash, salt FROM users WHERE username = ?";
        connection.query(sql, [username], (err, results) => {
            connection.end();

            if (err) {
                console.error("DB query error:", err);
                return res.status(500).send("Internal server error");
            }

            if (results.length === 0) {
                return res.status(401).send("Invalid username or password");
            }

            const { password_hash, salt } = results[0];
            const hash = crypto
                .createHash("sha256")
                .update(salt + password)
                .digest("hex");

            const hashBuf = Buffer.from(hash, "hex");
            const storedBuf = Buffer.from(password_hash, "hex");
            const valid = hashBuf.length === storedBuf.length &&
                crypto.timingSafeEqual(hashBuf, storedBuf);

            if (!valid) {
                return res.status(401).send("Invalid username or password");
            }

            // TODO: create a session or issue a JWT
            res.send("Login successful");
        });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
