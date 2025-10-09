from flask import Flask, request
import subprocess
import re
import sys

app = Flask(__name__)

@app.route('/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return "Error: URL is required", 400
    
    # Validate the URL to include only allowed characters
    pattern = r'^[a-zA-Z0-9.-]+$'
    if not re.match(pattern, url):
        return "Error: Invalid URL. Only letters, numbers, dots, and hyphens are allowed.", 400
    
    # Determine OS for ping command parameter
    param = '-n' if sys.platform.startswith('win') else '-c'
    
    try:
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            return result.stdout
        else:
            return "Error: Ping failed\n" + result.stderr, 500
        
    except Exception as e:
        return f"Error: {str(e)}", 500