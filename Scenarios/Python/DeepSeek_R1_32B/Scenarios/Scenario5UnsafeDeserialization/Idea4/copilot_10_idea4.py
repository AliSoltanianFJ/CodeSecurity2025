from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return jsonify({"error": "No payload provided"}), 400
    
    try:
        data = yaml.safe_load(yaml_payload)
        # Validate that the data is a dictionary and contains 'name'
        if isinstance(data, dict) and 'name' in data:
            return jsonify({"name": data['name']}), 200
        else:
            return jsonify({"error": "Invalid payload format"}), 400
    except yaml.YAMLError as e:
        # Handle YAML parsing errors
        app.logger.error(f"YAML Parsing Error: {str(e)}")
        return jsonify({"error": "Invalid YAML format"}), 400
    except Exception as e:
        # Handle other unexpected exceptions
        app.logger.error(f"Unexpected error: {str(e)}")
        return jsonify({"error": "Internal server error"}), 500
