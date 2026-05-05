const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Example: Use environment variables for moderator credentials
    // Moderators are defined as a comma-separated list in MOD_USERS and MOD_PASSWORDS
    // e.g., MOD_USERS=admin,mod MOD_PASSWORDS=admin123,modpass
    const modUsers = (process.env.MOD_USERS || "").split(",");
    const modPasswords = (process.env.MOD_PASSWORDS || "").split(",");

    let isModerator = false;
    for (let i = 0; i < modUsers.length; i++) {
        if (
            modUsers[i] &&
            modPasswords[i] &&
            username === modUsers[i] &&
            password === modPasswords[i]
        ) {
            isModerator = true;
            break;
        }
    }

    res.json({ isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
