from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
import json

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Define allowed schemes for URL validation
    allowed_schemes = ['http', 'https']
    
    # Get the URL from request arguments
    url = request.args.get('url')
    
    # Check if URL is provided and not empty
    if not url:
        return json.dumps({'error': 'URL parameter is required'}), 400
    
    # Validate URL format using a regex pattern
    url_pattern = re.compile(
        r'^(?:http|https)://'  # Scheme
        r'(?:[a-zA-Z0-9-]+\.)*'  # Subdomains
        r'[a-zA-Z0-9-]+'  # Domain name
        r'\.(?:[a-zA-Z]{2,6})'  # Top-level domain
        r'(?::\d{1,5})?'  # Optional port number
        r'(?:/[^\s]*)?$'  # Path (optional)
    )
    
    if not url_pattern.match(url):
        return json.dumps({'error': 'Invalid URL format'}), 400
    
    # Check for allowed schemes
    scheme = url.split('://')[0].lower()
    if scheme not in allowed_schemes:
        return json.dumps({'error': 'Only HTTP and HTTPS URLs are allowed'}), 400
    
    try:
        # Use subprocess to run the ping command safely
        # We use a list for the command to prevent shell injection
        result = subprocess.run(
            ['ping', '-c', '3', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10  # Set a reasonable timeout
        )
        
        if result.returncode == 0:
            return json.dumps({'status': 'success', 'result': result.stdout})
        else:
            error_msg = f'Ping failed: {result.stderr.strip()}'
            return json.dumps({'error': error_msg}), 400
    
    except subprocess.TimeoutExpired:
        return json.dumps({'error': 'Request timed out'}), 503
    except ValueError:
        return json.dumps({'error': 'Invalid URL'}), 400
    except Exception as e:
        return json.dumps({'error': str(e)}), 500

