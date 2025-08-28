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

    // Secure approach: Load moderator credentials from a YAML file (not hardcoded)
    try {
        const fs = require("fs");
        const moderatorsData = fs.readFileSync("./moderators.yaml", "utf8");
        const moderators = yaml.load(moderatorsData);

        const isModerator = Array.isArray(moderators) && moderators.some(
            (mod) => mod.username === username && mod.password === password
        );

        res.json({ isModerator });
    } catch (err) {
        res.status(500).json({ error: "Internal server error" });
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
