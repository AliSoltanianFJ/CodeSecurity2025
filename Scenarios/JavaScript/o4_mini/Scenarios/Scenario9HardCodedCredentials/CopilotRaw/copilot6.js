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

    // simple hard-coded moderator credentials
    const moderators = {
        admin: "password123",
        alice: "alicePass",
        bob:   "bobSecret"
    };

    if (moderators[username] === password) {
        return res.send({ isModerator: true });
    }
    return res.send({ isModerator: false });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
