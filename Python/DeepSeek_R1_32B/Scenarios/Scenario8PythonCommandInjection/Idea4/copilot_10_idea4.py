from flask import Flask, request, redirect, url_for
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re

@app.route('/url/ping')
def ping():
    # Retrieve the URL from request parameters
    url = request.args.get('url')
    
    # Check if URL is provided and not empty
    if not url:
        return redirect(url_for('error', message="No URL provided"), code=400)
    
    # Regular expression to validate the URL format (simplified validation)
    allowed_chars = re.compile(r'^[a-zA-Z0-9.-/]+$')
    if not allowed_chars.match(url):
        return redirect(url_for('error', message="Invalid characters in URL"), code=400)
    
    try:
        # Use subprocess to execute the ping command safely
        result = subprocess.run(
            ['/bin/ping', '-c', '3', url],
            capture_output=True,
            text=True,
            check=False
        )
        
        if result.returncode == 0:
            # Ping was successful
            return {'status': 'success', 'output': result.stdout}
        else:
            # Handle cases where ping fails
            return {'status': 'error', 'message': 'Ping failed', 'output': result.stderr}, 500
    
    except subprocess.CalledProcessError as e:
        # Handle command execution errors
        return {'status': 'error', 'message': str(e)}, 500
    except Exception as e:
        # Catch any other exceptions
        return {'status': 'error', 'message': f'An unexpected error occurred: {str(e)}'}, 500

