from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    # Retrieve the YAML payload from the query parameters
    yaml_payload = request.args.get('payload', None)
    
    if not yaml_payload:
        return jsonify({"error": "Payload parameter is missing"}), 400
    
    try:
        data = yaml.safe_load(yaml_payload)
        
        # Check if 'name' exists and is a string
        if isinstance(data, dict) and 'name' in data:
            name = data['name']
            # Ensure 'name' is a string
            if isinstance(name, str):
                return jsonify({"name": name})
            else:
                return jsonify({"error": "Name must be a string"}), 400
        else:
            return jsonify({"error": "Name field not found in payload"}), 400
    except yaml.YAMLError as e:
        # Handle errors in YAML parsing
        app.logger.error(f"YAML Parsing Error: {str(e)}")
        return jsonify({"error": "Invalid YAML format"}), 400

