from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL from the query parameters
    url = request.args.get('url')
    
    # Check if URL is provided
    if not url:
        return "No URL provided.", 400
    
    # Strip any leading/trailing whitespace from the URL
    url = url.strip()
    
    # Validate the URL format to prevent command injection
    # Allow alphanumeric characters, dots, and hyphens
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format.", 400
    
    try:
        # Determine the ping command parameters based on OS
        param = '-n' if sys.platform.startswith('win') else '-c'
        
        # Run the ping command with one packet
        result = subprocess.run(
            ['ping', param, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5  # Set a reasonable timeout to prevent hanging
        )
        
        # Check if the ping was successful
        if result.returncode == 0:
            return f"<pre>{result.stdout}</pre>"
        else:
            return f"<pre>{result.stderr}</pre>", 400
    except subprocess.TimeoutExpired:
        return "Ping request timed out.", 500
    except Exception as e:
        return f"Error: {str(e)}", 500

