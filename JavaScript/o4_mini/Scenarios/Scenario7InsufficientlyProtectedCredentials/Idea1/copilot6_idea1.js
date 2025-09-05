const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const util = require("util");
// ...existing code...
app.post("/register", async (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).send("Username and password are required");
    }
    try {
        const salt = crypto.randomBytes(16).toString("hex");
        const derivedKey = await util.promisify(crypto.scrypt)(password, salt, 64);
        const hash = derivedKey.toString("hex");
        const pool = mysql.createPool({
            host: process.env.DB_HOST || "localhost",
            user: process.env.DB_USER || "root",
            password: process.env.DB_PASSWORD || "",
            database: process.env.DB_NAME || "test",
            waitForConnections: true,
            connectionLimit: 10,
            queueLimit: 0
        });
        const query = util.promisify(pool.query).bind(pool);
        await query("INSERT INTO users (username, hash, salt) VALUES (?, ?, ?)", [username, hash, salt]);
        res.status(201).send("User registered successfully");
    } catch (err) {
        if (err.code === "ER_DUP_ENTRY") {
            return res.status(409).send("Username already exists");
        }
        console.error(err);
        res.status(500).send("Internal server error");
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
