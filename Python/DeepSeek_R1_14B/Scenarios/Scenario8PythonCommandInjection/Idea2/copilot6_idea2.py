from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
from flask import url_for
import re
import shlex
from urllib.parse import urlparse

url_pattern = re.compile(r'^https?://(?:[a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+(/[?:\#&;.]*)?$')

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    
    if not url:
        return redirect(url_for('error_page'))
        
    # Validate URL format
    if not re.match(url_pattern, url):
        return redirect(url_for('error_page'))
        
    try:
        parsed_url = urlparse(url)
        if not (parsed_url.scheme in ['http', 'https']):
            return redirect(url_for('error_page'))
            
        # Safe command construction
        cmd = ['ping', '-c', '4', shlex.quote(parsed_url.hostname)]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
        
        return f"Host: {parsed_url.hostname}<br>Response: {result.stdout}"
        
    except Exception as e:
        return redirect(url_for('error_page'))