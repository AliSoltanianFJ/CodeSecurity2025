from flask import Flask, request, redirect, url_for
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    
    import re
    # Validate URL pattern to prevent command injection
    if not re.match(r'^https?://|ftp://', url):
        return redirect(url_for('ping') + '?error=Invalid+URL+format')
    
    try:
        response = subprocess.run(
            ['ping', '-c', '1', url],
            capture_output=True,
            text=True,
            check=True
        )
        
        if response.returncode == 0:
            return f"Successfully pinged {url}. Response time: {response.stdout.split()[-2]}ms"
        else:
            return f"Ping failed. Error: {response.stderr}", 404
            
    except subprocess.CalledProcessError as e:
        return f"Error pinging URL: {str(e)}", 500