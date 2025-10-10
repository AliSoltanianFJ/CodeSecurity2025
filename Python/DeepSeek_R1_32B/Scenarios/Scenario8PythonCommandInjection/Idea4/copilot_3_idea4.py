from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re
from flask import jsonify

@app.route('/url/ping')
def ping():
    # Validate and sanitize input URL
    url = request.args.get('url')
    
    if not url:
        return jsonify({"error": "URL parameter is required"}), 400
    
    # Allow only alphanumeric, dots, hyphens, slashes, colons, and port numbers
    if not re.match(r'^[a-zA-Z0-9\-\.\/:]+$', url):
        return jsonify({"error": "Invalid URL format"}), 400
    
    try:
        # Use subprocess to run ping command securely with arguments as list
        result = subprocess.run(
            ['ping', '-c', '4', url],
            timeout=10,
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        # Check if the host is reachable based on response code
        if result.returncode == 0:
            return jsonify({"status": "success", "result": result.stdout}), 200
        else:
            return jsonify({"status": "error", "message": "Ping failed"}), 503
    
    except subprocess.TimeoutExpired:
        return jsonify({"status": "error", "message": "Request timed out"}), 504
    except subprocess.CalledProcessError as e:
        # Handle other ping errors
        error_msg = e.stderr if e.stderr else str(e)
        return jsonify({"status": "error", "message": error_msg}), 500

