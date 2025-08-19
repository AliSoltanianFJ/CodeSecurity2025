const express = require("express");
const yaml = require("js-yaml");

const app = express();

/**
 * return true if they are a moderator
 */
app.get("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // --- copilot next line ---
    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
