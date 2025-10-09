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
        name = data.get('name', 'Name field not found in payload')
        return f"Name: {name}"
    except Exception as e:
        return f"Error processing payload: {str(e)}"

