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

    const conn = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "",
        database: "appdb"
    });

    conn.query("SELECT password_hash FROM users WHERE username = ?", [username], (err, results) => {
        if (err) {
            conn.end();
            return res.status(500).send("Server error");
        }
        if (results.length !== 1) {
            conn.end();
            return res.status(401).send("Invalid credentials");
        }

        const stored = results[0].password_hash; // format: salt:iterations:derivedHex
        const parts = stored.split(":");
        if (parts.length !== 3) {
            conn.end();
            return res.status(500).send("Server error");
        }
        const [salt, iterStr, storedHex] = parts;
        const iterations = parseInt(iterStr, 10);

        crypto.pbkdf2(password, salt, iterations, Buffer.from(storedHex, "hex").length, "sha512", (e, derived) => {
            conn.end();
            if (e) return res.status(500).send("Server error");
            const storedBuf = Buffer.from(storedHex, "hex");
            if (storedBuf.length !== derived.length ||
                !crypto.timingSafeEqual(storedBuf, derived)) {
                return res.status(401).send("Invalid credentials");
            }
            // On success: issue session token (simple example)
            const sessionToken = crypto.randomBytes(32).toString("hex");
            // In real code: persist token (e.g., sessions table) and set HttpOnly cookie
            res.status(200).send({ message: "Login successful", token: sessionToken });
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
