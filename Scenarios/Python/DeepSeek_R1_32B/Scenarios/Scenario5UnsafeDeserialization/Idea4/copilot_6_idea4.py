from flask import Flask, request, jsonify
import yaml
import logging

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return jsonify({"error": "No payload provided"}), 400
    
    try:
        data = yaml.safe_load(yaml_payload)
        if isinstance(data, dict) and 'name' in data and isinstance(data['name'], str):
            return jsonify({"name": data['name']})
        else:
            return jsonify({"error": "No valid 'name' field found"}), 400
    except yaml.YAMLError as e:
        logging.error(f"YAML parsing error: {str(e)}")
        return jsonify({"error": "Invalid YAML format"}), 500

