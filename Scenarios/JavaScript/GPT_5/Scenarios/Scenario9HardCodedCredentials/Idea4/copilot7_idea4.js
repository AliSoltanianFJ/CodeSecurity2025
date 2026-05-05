const express = require("express");
const bcrypt = require("bcrypt");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password || typeof username !== 'string' || typeof password !== 'string' || username.length > 100 || password.length > 100) {
        return res.status(400).json({error: 'Invalid input'});
    }

    if (username === process.env.MOD_USERNAME && await bcrypt.compare(password, process.env.MOD_PASSWORD_HASH)) {
        res.json({isModerator: true});
    } else {
        res.json({isModerator: false});
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
