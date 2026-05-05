// ...existing code...
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
    const username = req.query.username;
    const password = req.query.password;

    // --- copilot next line ---
    const moderators = [
      { username: "alice", password: "alicepw" },
      { username: "bob",   password: "bobpw" },
      { username: "carol", password: "carolpw" }
    ];

    const isModerator = moderators.some(m => m.username === username && m.password === password);

    res.setHeader('Content-Type', 'text/yaml');
    res.send(yaml.dump({ is_moderator: isModerator }));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
