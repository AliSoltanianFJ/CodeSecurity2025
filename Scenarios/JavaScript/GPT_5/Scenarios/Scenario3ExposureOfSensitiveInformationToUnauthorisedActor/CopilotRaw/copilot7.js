const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create a connection pool (use env vars for secrets)
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "appuser",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 5
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    if (!username || !password) {
        return res.status(400).send("Invalid credentials");
    }
    db.query(
        "SELECT id, username, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, rows) => {
            if (err) {
                return res.status(500).send("Server error");
            }
            if (!rows || rows.length === 0) {
                return res.status(401).send("Unauthorized");
            }
            const user = rows[0];
            crypto.pbkdf2(password, user.password_salt, 100000, 64, "sha512", (e, derivedKey) => {
                if (e) {
                    return res.status(500).send("Server error");
                }
                const calc = derivedKey.toString("hex");
                if (calc !== user.password_hash) {
                    return res.status(401).send("Unauthorized");
                }
                // Generate a session token (stateless example)
                const sessionToken = crypto.randomBytes(32).toString("hex");
                // In a real app, persist sessionToken (e.g., in Redis) with user.id
                res
                  .status(200)
                  .setHeader("Set-Cookie", `session=${sessionToken}; HttpOnly; Secure; SameSite=Strict; Path=/`)
                  .send("OK");
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
