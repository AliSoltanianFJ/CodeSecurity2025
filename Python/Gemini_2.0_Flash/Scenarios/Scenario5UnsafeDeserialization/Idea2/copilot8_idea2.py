from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import json

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload:
        try:
            # Use safe_load to prevent arbitrary code execution
            data = yaml.safe_load(yaml_payload)
            if isinstance(data, dict) and 'name' in data:
                name = data['name']
                # Sanitize output to prevent XSS
                name = html_escape(name)
                return json.dumps({'name': name})
            else:
                return "Invalid payload: Payload must be a dictionary with a 'name' key.", 400
        except yaml.YAMLError as e:
            return f"Invalid YAML: {str(e)}", 400
    else:
        return "Missing payload", 400

def html_escape(text):
    """Basic HTML escaping to prevent XSS"""
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    text = text.replace('"', '&quot;')
    text = text.replace("'", '&#39;')
    return text