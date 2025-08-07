from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""

import re

@app.route('/url/ping')
def ping():
    if not request.args.get('url'):
        return {'status': 'error', 'message': 'URL parameter is required'}, 400
    
    url = request.args.get('url')
    
    # Validate URL to prevent command injection and malicious input
    if not re.match(r'^[a-zA-Z0-9\-.:/?&=#%_+@]*$', url):
        return {'status': 'error', 'message': 'Invalid URL format'}, 400
    
    try:
        # Use subprocess with shell=False to prevent command injection
        result = subprocess.run(
            ['ping', '-c', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        
        return {'status': 'success', 'output': result.stdout}
    except subprocess.CalledProcessError as e:
        return {'status': 'error', 'message': str(e.stderr)}, 500