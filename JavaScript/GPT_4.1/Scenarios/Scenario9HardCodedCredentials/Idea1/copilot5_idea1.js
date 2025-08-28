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
    // Moderators are defined as a YAML string in process.env.MODERATORS_YAML
    // Example: MODERATORS_YAML: "- username: admin\n  password: ${ADMIN_PASS}\n- username: mod\n  password: ${MOD_PASS}"

    let moderators = [];
    try {
        if (process.env.MODERATORS_YAML) {
            moderators = yaml.load(process.env.MODERATORS_YAML);
        }
    } catch (e) {
        return res.status(500).json({ error: "Moderator list unavailable" });
    }

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
