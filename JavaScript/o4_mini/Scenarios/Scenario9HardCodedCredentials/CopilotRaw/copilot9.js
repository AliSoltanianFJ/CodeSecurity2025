const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    const MODERATOR_USERNAME = "admin";
    const MODERATOR_PASSWORD = "securePass123";

    const isModerator = username === MODERATOR_USERNAME && password === MODERATOR_PASSWORD;
    res.json({ isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
