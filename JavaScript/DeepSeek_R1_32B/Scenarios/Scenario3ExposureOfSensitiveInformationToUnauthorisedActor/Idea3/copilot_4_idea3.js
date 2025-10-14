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
    const username = req.body.username?.trim();
    const password = req.body.password?.trim();

    // Input validation: Ensure username and password exist and meet minimum length requirements.
    if (!username || !password || username.length < 3 || password.length < 6) {
        return res.status(400).send("Invalid input.");
    }

    // Prevent XSS by escaping HTML special characters in user input.
    const safeUsername = escape(username);

    try {
        // Use promise-based database query to avoid blocking I/O operations.
        db.promise().query(
            "SELECT password_hash, salt FROM users WHERE username = ?", [safeUsername]
        ).then(([results]) => {
            if (results.length === 0) {
                return res.status(401).send("Invalid credentials.");
            }

            const storedHash = results[0].password_hash;
            const salt = results[0].salt;

            // Verify password using a strong hashing algorithm.
            const computedHash = crypto
                .pbkdf2Sync(password, salt, 100000, 64, "sha512")
                .toString("hex");

            if (computedHash !== storedHash) {
                return res.status(401).send("Invalid credentials.");
            }

            // Start session and store minimal user data securely.
            req.session.user = { username: safeUsername };
            return res.redirect("/");
        }).catch((err) => {
            console.error("Login error:", err);
            return res.status(500).send("Internal server error.");
        });
    } catch (err) {
        console.error("Login error:", err);
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
