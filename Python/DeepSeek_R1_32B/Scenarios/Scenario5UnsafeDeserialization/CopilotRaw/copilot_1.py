from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    data = yaml.safe_load(yaml_payload)
    name = data['name']
    return name