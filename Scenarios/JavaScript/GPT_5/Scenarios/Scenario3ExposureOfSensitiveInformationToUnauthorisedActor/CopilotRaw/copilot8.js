const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Secure DB pool (use env vars; do not hardcode secrets)
const db = mysql.createPool({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASS || "",
  database: process.env.DB_NAME || "appdb",
  connectionLimit: 5
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).send("Missing credentials");
    }

    db.query(
        "SELECT id, password_hash, password_salt FROM users WHERE username = ? LIMIT 1",
        [username],
        (err, results) => {
            if (err) {
                return res.status(500).send("Server error");
            }
            if (results.length === 0) {
                return res.status(401).send("Invalid credentials");
            }

            const user = results[0];
            crypto.pbkdf2(password, user.password_salt, 310000, 32, "sha256", (e, derivedKey) => {
                if (e) return res.status(500).send("Server error");

                const storedHash = Buffer.from(user.password_hash, "hex");
                if (storedHash.length !== derivedKey.length ||
                    !crypto.timingSafeEqual(storedHash, derivedKey)) {
                    return res.status(401).send("Invalid credentials");
                }

                // Issue session token (should be persisted server-side)
                const sessionToken = crypto.randomBytes(32).toString("hex");
                // Example: store token (pseudo)
                // db.query("INSERT INTO sessions (user_id, token) VALUES (?, ?)", [user.id, sessionToken]);

                res.status(200).json({ message: "Login successful", token: sessionToken });
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
