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

    // Example: Securely check moderator status using environment variables
    // Moderators are defined as a comma-separated list in process.env.MODERATORS
    // Each moderator is in the format username:password
    // e.g., MODERATORS=admin:admin123,mod:modpass

    const moderatorsEnv = process.env.MODERATORS || "";
    const moderators = moderatorsEnv.split(",").map(entry => {
        const [user, pass] = entry.split(":");
        return { username: user, password: pass };
    });

    const isModerator = moderators.some(
        (mod) => mod.username === username && mod.password === password
    );

    res.json({ isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
