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

    // hard-coded moderator credentials
    const moderators = {
        "alice": "alicePassword",
        "bob": "bobPassword"
    };

    if (moderators[username] && moderators[username] === password) {
        return res.json(true);
    }
    return res.json(false);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
