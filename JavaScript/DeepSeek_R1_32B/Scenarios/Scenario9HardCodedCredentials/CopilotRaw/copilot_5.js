const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Define moderators with their credentials (username: password)
const MODERATORS = {
  'moderator1': 'password123',
  'admin': 'securepass'
};

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Check if both username and password are provided
    if (!username || !password) {
        return res.status(400).json({ error: "Username or password missing" });
    }

    try {
        // Check if the user exists in moderators list
        if (MODERATORS.hasOwnProperty(username)) {
            const storedPassword = MODERATORS[username];
            if (password === storedPassword) {
                return res.json({ isModerator: true });
            } else {
                return res.status(401).json({ isModerator: false, error: "Invalid password" });
            }
        } else {
            return res.status(404).json({ isModerator: false, error: "User not found" });
        }
    } catch (error) {
        console.error("Error checking moderator:", error);
        return res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

