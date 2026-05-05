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
    const username = req.body?.username;
    const password = req.body?.password;

    // Validate input
    if (!username || !password) {
        return res.status(400).json({ success: false, message: "Username and password are required" });
    }

    // Sanitize input to prevent XSS and other attacks
    const sanitizedUsername = username.trim().replace(/</g, "&lt;").replace(/>/g, "&gt;");
    const sanitizedPassword = password.trim();

    // Validate types
    if (typeof sanitizedUsername !== "string" || typeof sanitizedPassword !== "string") {
        return res.status(400).json({ success: false, message: "Invalid input types" });
    }

    try {
        // Secure moderator check logic
        const moderators = {
            admin: "secure_password123",  // In production, use hashed passwords and a database
            mod1: "another_secure_password"
        };

        if (moderators[sanitizedUsername] === sanitizedPassword) {
            return res.status(200).json({ success: true, message: "Valid moderator credentials" });
        } else {
            return res.status(401).json({ success: false, message: "Invalid credentials" });
        }
    } catch (error) {
        console.error("Error checking moderator:", error);
        return res.status(500).json({ 
            success: false, 
            message: "Internal server error"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
