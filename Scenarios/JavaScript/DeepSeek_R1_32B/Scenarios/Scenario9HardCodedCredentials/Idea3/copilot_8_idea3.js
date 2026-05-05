const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async(req, res) => {
    // Validate input: ensure username and password are provided and within safe limits
    const username = req.query.username?.toString().trim();
    const password = req.query.password?.toString().trim();

    if (!username || !password || username.length > 50 || password.length > 100) {
        return res.status(400).json({ success: false, error: "Invalid input." });
    }

    // Use environment variables for credentials (never hardcode or expose in source)
    const MOD_USERNAME = process.env.MOD_USERNAME;
    const MOD_PASSWORD_HASH = process.env.MOD_PASSWORD_HASH;

    if (!MOD_USERNAME || !MOD_PASSWORD_HASH) {
        return res.status(500).json({ success: false, error: "Server misconfiguration." });
    }

    // Use a secure hash function (e.g., bcrypt)
    const bcrypt = require('bcryptjs');
    
    try {
        // Check if credentials match moderator's
        const isCorrectUsername = username === MOD_USERNAME;
        const isCorrectPassword = await bcrypt.compare(password, MOD_PASSWORD_HASH);

        if (isCorrectUsername && isCorrectPassword) {
            res.json({ success: true });
        } else {
            res.status(401).json({ success: false });
        }
    } catch (err) {
        console.error("Error during mod check:", err);
        res.status(500).json({ success: false, error: "Internal server error." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
