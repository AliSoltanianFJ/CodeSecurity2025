const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (!username || typeof username !== "string" || !password || typeof password !== "string") {
        return res.status(400).send("Username and password are required.");
    }
    if (password.length < 8) {
        return res.status(400).send("Password must be at least 8 characters long.");
    }

    crypto.randomBytes(16, (saltErr, saltBuf) => {
        if (saltErr) {
            console.error("Salt generation error:", saltErr);
            return res.status(500).send("Internal server error");
        }
        const salt = saltBuf.toString("hex");
        crypto.scrypt(password, salt, 64, (hashErr, derivedKey) => {
            if (hashErr) {
                console.error("Hashing error:", hashErr);
                return res.status(500).send("Internal server error");
            }
            const hash = derivedKey.toString("hex");
            const pool = mysql.createPool({
                host: process.env.DB_HOST,
                user: process.env.DB_USER,
                password: process.env.DB_PASSWORD,
                database: process.env.DB_NAME,
                waitForConnections: true,
                connectionLimit: 10,
                queueLimit: 0,
                ssl: { rejectUnauthorized: true }
            });
            const sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            pool.execute(sql, [username, hash, salt], (dbErr) => {
                if (dbErr) {
                    if (dbErr.code === "ER_DUP_ENTRY") {
                        return res.status(409).send("Username already exists");
                    }
                    console.error("DB insertion error:", dbErr);
                    return res.status(500).send("Internal server error");
                }
                res.status(201).send("User registered successfully");
            });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
