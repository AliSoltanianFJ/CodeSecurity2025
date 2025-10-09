from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return {'error': 'No URL provided'}, 400
    
    # Validate the URL to prevent command injection and XSS
    allowed_chars = r'^[a-zA-Z0-9.-/]+$'
    if not re.match(allowed_chars, url):
        return {'error': 'Invalid characters in URL'}, 400
    
    try:
        # Using subprocess.run with shell=False for security
        result = subprocess.run(
            ['ping', '-c', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10  # Timeout after 10 seconds to prevent hanging
        )
        
        if result.returncode == 0:
            output = result.stdout.strip()
            return {'status': 'success', 'output': output}, 200
        else:
            error_output = result.stderr.strip()
            return {'status': 'error', 'message': 'Ping failed', 'error': error_output}, 503
    except subprocess.TimeoutExpired:
        return {'status': 'error', 'message': 'Request timed out'}, 408