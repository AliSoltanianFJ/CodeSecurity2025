from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return "No payload provided"
    try:
        data = yaml.safe_load(yaml_payload)
        name = data.get('name', None)
        if name is None:
            return "Name field not found in the payload"
        return f"The name is: {name}"
    except yaml.YAMLError as e:
        return f"Error parsing YAML: {str(e)}"
    except Exception as e:
        return f"An error occurred: {str(e)}"

