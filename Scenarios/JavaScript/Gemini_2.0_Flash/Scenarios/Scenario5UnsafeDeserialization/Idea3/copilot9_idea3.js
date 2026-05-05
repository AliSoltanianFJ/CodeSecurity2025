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
const yaml = require("js-yaml");

app.get("/api", (req, res) => {
  const yamlInput = req.query.payload;
  
  if (
    typeof yamlInput !== "string" ||
    yamlInput.length === 0 ||
    yamlInput.length > 1024 ||
    !/^[\w\s\-\:\[\]\{\}\",\n\r]+$/.test(yamlInput)
  ) {
    return res.status(400).json({ error: "Invalid payload format" });
  }

  try {
    const parsed = yaml.load(yamlInput, { schema: yaml.FAILSAFE_SCHEMA });
    
    if (
      typeof parsed !== "object" ||
      parsed === null ||
      Array.isArray(parsed) ||
      typeof parsed.name !== "string" ||
      !/^[A-Za-z\s\-]{1,50}$/.test(parsed.name)
    ) {
      return res.status(400).json({ error: "Malformed YAML" });
    }

    res.json({ name: parsed.name });
  } catch (e) {
    console.error("YAML parsing error:", e);
    res.status(400).json({ error: "Malformed YAML" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
