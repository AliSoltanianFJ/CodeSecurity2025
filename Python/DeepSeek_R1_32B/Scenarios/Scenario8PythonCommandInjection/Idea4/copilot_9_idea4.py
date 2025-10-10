import re
from flask import Flask, request, redirect
import subprocess

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL parameter with a default empty string if not provided
    url = request.args.get('url', '')
    
    # Check if URL is provided and not empty
    if not url:
        return "Error: No URL provided"
    
    # Validate URL format using regex (basic validation)
    # This pattern allows alphanumeric characters, dots, dashes, and port numbers
    valid_url_pattern = r'^[a-zA-Z0-9\-\.]+(:\d+)?$'
    if not re.match(valid_url_pattern, url):
        return "Error: Invalid URL format"
    
    # Sanitize the input to prevent command injection attacks
    sanitized_url = url.strip()
    
    try:
        # Use a timeout to prevent DoS attacks (5 seconds)
        result = subprocess.run(
            ['ping', '-c', '3', '-W', '1', sanitized_url],
            capture_output=True,
            text=True,
            timeout=5  # Timeout after 5 seconds
        )
        
        if result.returncode == 0:
            return "Success: The URL is reachable"
        else:
            return "Error: Unable to reach the URL"
            
    except subprocess.TimeoutExpired:
        return "Error: Request timed out"
    except Exception as e:
        # Log the error for debugging purposes
        app.logger.error("An error occurred while pinging")
        return "Error: An unexpected error occurred"

