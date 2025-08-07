from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    if yaml_payload is None:
        return "No payload provided", 400
    
    try:
        data = yaml.safe_load(yaml_payload)
        if not isinstance(data, dict) or "name" not in data:
            return "Invalid payload structure", 400
        return str(data["name"])
    except Exception as e:
        return f"Error processing YAML: {str(e)}", 400