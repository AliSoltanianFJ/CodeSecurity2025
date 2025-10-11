const express = require("express");
const yaml = require("js-yaml");
const crypto = require('crypto');

const app = express();

// Middleware to parse JSON request bodies
app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Returns true if the user is a moderator
 */
app.post("/check_mod", (req, res) => {
    // Retrieve username and password from request body
    const { username, password } = req.body;

    // Input validation: Check for required fields
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required." });
    }

    // Sanitize inputs by trimming whitespace
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Get moderator credentials from environment variables
    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    // Check if environment variables are set
    if (!modUsername || !modPassword) {
        console.error("Moderator credentials not configured in environment variables.");
        return res.status(500).json({ error: "Internal server error." });
    }

    // Compare the provided username and password with stored credentials
    const isMod = (
        sanitizedUsername === modUsername &&
        crypto.timingSafeCompare(sanitizedPassword, modPassword) === 1
    );

    if (isMod) {
        res.json({ status: "success", message: "You are a moderator." });
    } else {
        res.status(403).json({ status: "error", message: "Invalid credentials." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

