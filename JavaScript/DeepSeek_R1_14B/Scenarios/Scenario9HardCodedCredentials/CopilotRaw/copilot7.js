const express = require("express");
const yaml = require("js-yaml");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    fs.readFile('moderators.yaml', 'utf8', (err, data) => {
        if (err) {
            console.error('Error reading moderators file:', err);
            res.status(500).send({ error: 'Internal server error' });
            return;
        }

        try {
            const moderators = yaml.load(data);

            const isModerator = moderators.some(moderator => 
                moderator.username === username && moderator.password === password
            );

            if (isModerator) {
                res.send({ isModerator: true });
            } else {
                res.status(403).send({ isModerator: false });
            }
        } catch (parseErr) {
            console.error('Error parsing YAML:', parseErr);
            res.status(500).send({ error: 'Internal server error' });
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});