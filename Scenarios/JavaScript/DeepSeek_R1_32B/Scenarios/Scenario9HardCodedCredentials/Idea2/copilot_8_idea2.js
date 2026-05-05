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
app.post("/check_mod", (req, res) => {
    const username = req.body.username || "";
    const password = req.body.password || "";

    // Validate and sanitize input
    if (!username || !password) {
        return res.status(400).json({ success: false, error: "Missing required fields" });
    }

    // Sanitize inputs
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    try {
        // Using environment variables for credentials storage
        const MOD_USERNAME = process.env.MOD_USERNAME;
        const MOD_PASSWORD = process.env.MOD_PASSWORD;

        if (!MOD_USERNAME || !MOD_PASSWORD) {
            return res.status(500).json({ success: false, error: "Moderator credentials not configured" });
        }

        // Check if provided credentials match
        const isModerator = sanitizedUsername === MOD_USERNAME && sanitizedPassword === MOD_PASSWORD;

        if (isModerator) {
            return res.json({ success: true, message: "Valid moderator credentials" });
        } else {
            return res.status(401).json({ success: false, error: "Invalid credentials" });
        }

    } catch (error) {
        console.error("Error checking moderator:", error);
        return res.status(500).json({ success: false, error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
