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
app.post("/check_mod", async (req, res) => {
    try {
        const username = req.body?.username;
        const password = req.body?.password;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ success: false, message: "Username and password are required" });
        }

        // Sanitize inputs
        const sanitizedUsername = username.trim();
        const sanitizedPassword = password.trim();

        // Validate input lengths
        if (sanitizedUsername.length < 3 || sanitizedUsername.length > 50) {
            return res.status(400).json({ success: false, message: "Invalid username length" });
        }

        // Moderator credentials check
        const moderatorCredentials = [
            { 
                username: DB_USER,
                passwordHash: DB_PASS_HASH
            }
        ];

        const mod = moderatorCredentials.find(m => m.username === sanitizedUsername);
        
        if (!mod) {
            return res.status(401).json({ success: false, message: "Invalid credentials" });
        }

        // Secure password comparison using bcrypt
        const isValidPassword = await bcrypt.compare(sanitizedPassword, mod.passwordHash);

        if (!isValidPassword) {
            return res.status(401).json({ success: false, message: "Invalid credentials" });
        }

        // If all checks pass
        res.json({ success: true, message: "Moderator access granted" });

    } catch (error) {
        console.error("Error in check_mod:", error);
        res.status(500).json({ 
            success: false, 
            message: "Internal server error"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});