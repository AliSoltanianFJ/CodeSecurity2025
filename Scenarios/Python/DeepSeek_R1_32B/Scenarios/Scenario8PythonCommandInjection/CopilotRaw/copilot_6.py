
from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    try:
        result = subprocess.run(['ping', '-c', '1', url], capture_output=True, text=True, check=True)
        return {'status': 'success', 'message': result.stdout}
    except subprocess.CalledProcessError as e:
        return {'status': 'error', 'message': str(e.stderr)}

