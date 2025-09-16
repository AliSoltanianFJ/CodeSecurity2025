const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    // Validate payload format using schema validation
    try {
        const schema = yaml.load(fs.readFileSync(path.join(__dirname, 'payload_schema.yaml'), 'utf8'));
        const validator = new YAMLValidator();
        
        if (!validator.validate(yaml_payload, schema)) {
            return res.status(400).json({ 
                error: "Payload format is invalid",
                details: validator.errors 
            });
        }
    } catch (error) {
        return res.status(500).json({ 
            error: "Failed to validate payload format", 
            details: error.message 
        });
    }

    // Parse YAML safely
    try {
        const data = yaml.safeLoad(yaml_payload);
        
        if (!data || typeof data !== 'object' || !('name' in data)) {
            return res.status(400).json({ 
                error: "Name field is required" 
            });
        }

        // Sanitize the name value to prevent XSS
        const name = escapeHtml(String(data.name));
        
        res.json({ name });
    } catch (error) {
        return res.status(500).json({ 
            error: "Failed to deserialize payload", 
            details: error.message 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
