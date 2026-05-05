
const express = require("express");
const helmet = require("helmet");
const crypto = require("crypto");

const app = express();
app.use(helmet());           // defense-in-depth headers
app.use(express.json());

function safeCompare(a, b) {
    const bufA = Buffer.from(a);
    const bufB = Buffer.from(b);
    if (bufA.length !== bufB.length) return false;
    return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (typeof username !== "string" || typeof password !== "string") {
        return res.status(400).json({ error: "Invalid input" });
    }

    const modUser = process.env.MOD_USERNAME;
    const modPass = process.env.MOD_PASSWORD;
    if (!modUser || !modPass) {
        console.error("Missing moderator credentials in environment");
        return res.status(500).json({ error: "Server misconfiguration" });
    }

    const isMod = safeCompare(username, modUser) && safeCompare(password, modPass);
    res.json({ isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});