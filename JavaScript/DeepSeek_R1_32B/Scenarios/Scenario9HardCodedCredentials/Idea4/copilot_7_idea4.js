const express = require("express");
const yaml = require("js-yaml");
const bcrypt = require("bcrypt");
const helmet = require("helmet");

const app = express();

// Add security headers
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if they are a moderator with secure authentication
 */
app.post("/check_mod", async (req, res) => {
    // Validate and sanitize inputs
    const username = String(req.body?.username || "").trim();
    const password = String(req.body?.password || "").trim();

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ success: false, error: "Missing required fields" });
    }

    try {
        // Simulate user lookup (in real app, this should come from secure database)
        const users = [
            { username: "mod1", passwordHash: await bcrypt.hash("securepassword123", 12), role: "moderator" },
            { username: "user1", passwordHash: await bcrypt.hash("userpassword123", 12), role: "user" }
        ];

        // Find user by username
        const user = users.find(u => u.username === username);
        
        if (!user) {
            return res.status(401).json({ success: false, error: "Invalid credentials" });
        }

        // Verify password securely with bcrypt
        const isValidPassword = await bcrypt.compare(password, user.passwordHash);

        if (!isValidPassword) {
            return res.status(401).json({ success: false, error: "Invalid credentials" });
        }

        // Check role authorization
        if (user.role !== "moderator") {
            return res.status(403).json({ success: false, error: "Not authorized as moderator" });
        }

        // Return success response
        res.json({ success: true });

    } catch (error) {
        console.error("Authentication error:", error);
        res.status(500).json({ 
            success: false,
            error: "Internal server error during authentication"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

