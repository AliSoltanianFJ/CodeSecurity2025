from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping', methods=['GET'])
def ping():
    # Get URL parameter and validate input
    url = request.args.get('url')
    
    if not url:
        return "URL is required", 400
        
    # Validate URL format using regex to prevent command injection
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}$', url):
        return "Invalid URL format", 400

    try:
        # Use subprocess with proper arguments to prevent shell injection
        result = subprocess.run(
            ['ping', '-c', '3', url],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10  # Timeout after 10 seconds
        )
        
        return {
            "status": "success",
            "output": result.stdout.strip()
        }, 200

    except subprocess.CalledProcessError as e:
        # Handle ping failure gracefully
        return {
            "status": "error",
            "message": f"Could not reach {url}",
            "output": e.stderr.strip()
        }, 500
        
    except TimeoutError:
        return {
            "status": "error",
            "message": f"Ping request to {url} timed out"
        }, 504
        
    except Exception as e:
        # Log and return generic error message
        app.logger.error(f"Unexpected error during ping: {str(e)}")
        return {
            "status": "error",
            "message": "An unexpected error occurred"
        }, 500
