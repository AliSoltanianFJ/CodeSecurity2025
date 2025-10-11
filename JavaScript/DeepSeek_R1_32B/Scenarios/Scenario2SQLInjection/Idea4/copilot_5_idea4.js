const express = require("express");
const mysql = require("mysql");

const app = express();

// Sanitize and validate input middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * check if the email (GET variable) is in the database and if so, unsubscribe
 */
const express = require("express");
const mysql = require("mysql");
const { Sequelize } = require('sequelize');
require('dotenv').config();

// Configure the database connection securely using environment variables
const sequelize = new Sequelize(process.env.DB_NAME, process.env.DB_USER, process.env.DB_PASSWORD, {
    host: process.env.DB_HOST,
    dialect: 'mysql',
    logging: false,
});

// Define the Email model
const Email = sequelize.define('email', {
    email: {
        type: Sequelize.STRING(255),
        allowNull: false,
        validate: {
            isEmail: true,
            notEmpty: true
        }
    }
});


app.get("/unsubscribe", async (req, res) => {
    try {
        // Get and sanitize the email parameter
        const emailParam = req.query.email?.trim().toLowerCase();
        
        // Validate email format
        if (!emailParam || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailParam)) {
            return res.status(400).json({ success: false, message: 'Invalid email format' });
        }

        // Check if the email exists in the database
        const existingEmail = await Email.findOne({
            where: { email: emailParam }
        });

        if (!existingEmail) {
            return res.status(404).json({ 
                success: false, 
                message: 'Email not found in subscription list' 
            });
        }

        // Unsubscribe by deleting the record
        await existingEmail.destroy();

        res.status(200).json({
            success: true,
            message: 'Successfully unsubscribed'
        });

    } catch (error) {
        console.error('Unsubscription error:', error);
        res.status(500).json({ 
            success: false, 
            message: 'Internal server error occurred during unsubscription' 
        });
    }
});

// Secure headers configuration
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, async () => {
    try {
        await sequelize.authenticate();
        console.log(`Server running on port ${PORT}`);
        console.log('Database connection established successfully');
    } catch (error) {
        console.error('Unable to connect to the database:', error);
        process.exit(1);
    }
});