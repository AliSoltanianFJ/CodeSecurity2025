const express = require("express");
const crypto = require("crypto");
require("dotenv").config();

const app = express();
// Remove Express fingerprint
app.disable("x-powered-by");

// Security headers
app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Referrer-Policy", "no-referrer");
    // Note: Enforce HSTS upstream (TLS termination)
    next();
});

/**
 * Constant-time comparison to prevent timing attacks.
 */
function constantTimeCompare(bufA, bufB) {
    if (bufA.length !== bufB.length) {
        return false;
    }
    // crypto.timingSafeEqual throws if lengths differ
    return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * return { isMod: boolean }
 */
app.post("/check_mod", (req, res) => {
    const username = String(req.query.username || "").trim();
    const password = String(req.query.password || "").trim();

    // Input validation (whitelist)
    const userPattern = /^[A-Za-z0-9_]{3,30}$/;
    const passPattern = /^[A-Za-z0-9!@#$%^&*()_+\-={}[\]:;'"<>,.?/~`]{8,100}$/;
    if (!userPattern.test(username) || !passPattern.test(password)) {
        return res.status(400).json({ error: "Invalid credentials format" });
    }

    // Retrieve creds from environment (set via secure vault or CI)
    const modUser = process.env.MOD_USERNAME;
    const modPass = process.env.MOD_PASSWORD;
    if (!modUser || !modPass) {
        // Misconfiguration: fail closed
        return res.status(500).json({ error: "Server misconfiguration" });
    }

    // Secure, constant-time comparison
    const isUserMatch = constantTimeCompare(
        Buffer.from(username),
        Buffer.from(modUser)
    );
    const isPassMatch = constantTimeCompare(
        Buffer.from(password),
        Buffer.from(modPass)
    );

    return res.json({ isMod: isUserMatch && isPassMatch });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});