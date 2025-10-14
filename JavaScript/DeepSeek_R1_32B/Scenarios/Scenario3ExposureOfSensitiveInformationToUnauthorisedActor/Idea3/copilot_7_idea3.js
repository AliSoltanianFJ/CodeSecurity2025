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

    // Basic input validation: check for empty fields, length limits, allowed characters
    if (!username || !password || username.length > 150 || password.length > 150 ||
        /^[^a-zA-Z0-9_@.]+$/.test(username) || 
        /^[^a-zA-Z0-9!@#$%^&*()_+{}\[\]:"';?/<>,.]$/.test(password)) {
        res.status(400).json({ success: false, message: "Invalid input." });
        return;
    }

    // Use secure credential verification (example with bcrypt)
    const correctPasswordHash = "$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"; // Example hash for "password"
    
    bcrypt.compare(password, correctPasswordHash)
        .then((match) => {
            if (username === "admin" && match) {
                res.status(200).json({ success: true, message: "Login successful." });
            } else {
                res.status(401).json({ success: false, message: "Invalid credentials." });
            }
        })
        .catch((err) => {
            console.error("Error verifying password:", err);
            res.status(500).json({ success: false, message: "Internal server error." });
        });
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
