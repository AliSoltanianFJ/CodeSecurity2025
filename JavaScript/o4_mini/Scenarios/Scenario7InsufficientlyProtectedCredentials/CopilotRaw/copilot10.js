
const express = require("express");
const mysql   = require("mysql");
const crypto  = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// �existing code�
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const pool = mysql.createPool({
    connectionLimit : 10,
    host            : process.env.DB_HOST,
    user            : process.env.DB_USER,
    password        : process.env.DB_PASSWORD,
    database        : process.env.DB_NAME
});


/**
 * register URL
 */
app.post("/register", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).send("Username and password are required.");
    }

    // generate a 16-byte salt
    const salt = crypto.randomBytes(16).toString("hex");
    // derive a 64-byte key with PBKDF2
    crypto.pbkdf2(password, salt, 100_000, 64, "sha512", (err, derivedKey) => {
        if (err) {
            console.error(err);
            return res.status(500).send("Internal error");
        }
        const hash = derivedKey.toString("hex");

        // parameterised query to avoid SQL injection
        pool.query(
            "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)",
            [username, salt, hash],
            (err) => {
                if (err) {
                    console.error(err);
                    return res.status(500).send("Could not register user.");
                }
                res.status(201).send("User registered successfully.");
            }
        );
    });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
