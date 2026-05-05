const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;
    // --- copilot next line ---
    if (!username || !password) return res.status(400).send("Invalid credentials");

    // Create (or preferably reuse) a connection/pool (placeholder values)
    const db = mysql.createConnection({
        host: process.env.DB_HOST || "localhost",
        user: process.env.DB_USER || "root",
        password: process.env.DB_PASS || "",
        database: process.env.DB_NAME || "appdb"
    });

    // Use a parameterized query to avoid SQL injection
    db.query(
        "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, results) => {
            if (err) {
                console.error(err);
                return res.status(500).send("Server error");
            }

            if (results.length === 0) {
                // Do not reveal whether username exists
                return res.status(401).send("Invalid credentials");
            }

            const user = results[0];
            try {
                const derived = crypto.pbkdf2Sync(
                    password,
                    user.password_salt,
                    310000,           // iterations
                    32,               // key length
                    "sha256"
                );

                const stored = Buffer.from(user.password_hash, "hex");
                if (
                    stored.length === derived.length &&
                    crypto.timingSafeEqual(stored, derived)
                ) {
                    // Generate a session token (placeholder)
                    const sessionToken = crypto.randomBytes(32).toString("hex");
                    // In production: persist token server-side and set secure cookie
                    res
                      .status(200)
                      .send(`Logged in (token: ${sessionToken})`);
                } else {
                    return res.status(401).send("Invalid credentials");
                }
            } catch (e) {
                console.error(e);
                return res.status(500).send("Server error");
            } finally {
                db.end?.();
            }
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
