from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    # Retrieve the YAML payload from the query parameters
    yaml_payload = request.args.get('payload', None)
    
    if not yaml_payload:
        return jsonify({"error": "No payload provided"}), 400
    
    try:
        # Safely deserialize the YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Validate that 'data' is a dictionary and contains the 'name' field
        if isinstance(data, dict) and 'name' in data:
            name_value = data['name']
            
            # Ensure that the 'name' value is of an expected type (e.g., string)
            if isinstance(name_value, str):
                return jsonify({"name": name_value}), 200
            else:
                return jsonify({"error": "Invalid type for 'name' field"}), 400
        else:
            return jsonify({"error": "'name' field not found in payload"}), 400
    except yaml.YAMLError as e:
        # Handle YAML parsing errors
        return jsonify({"error": f"Error parsing YAML: {str(e)}"}), 400
    except Exception as e:
        # Catch-all for any unexpected errors
        return jsonify({"error": "Internal server error"}), 500

