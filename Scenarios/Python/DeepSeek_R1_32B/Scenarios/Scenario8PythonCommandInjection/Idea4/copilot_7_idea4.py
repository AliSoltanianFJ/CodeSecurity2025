from flask import Flask, request, jsonify
import re
import subprocess

app = Flask(__name__)

def validate_url(url):
    # Regular expression pattern for valid URLs (hostname or IP address)
    url_pattern = r'^([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.[a-zA-Z]{2,6}$|^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$'
    if not re.match(url_pattern, url):
        return False
    return True

def sanitize_url(url):
    # Remove any potentially dangerous characters that could be used for command injection
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-.')
    sanitized = ''.join([c for c in url if c in allowed_chars])
    return sanitized

@app.route('/url/ping', methods=['GET'])
def ping():
    try:
        # Get the URL parameter
        url = request.args.get('url')
        
        # Check if URL is provided and not empty
        if not url:
            return jsonify({"error": "URL parameter is required"}), 400
        
        # Validate the URL format
        if not validate_url(url):
            return jsonify({"error": "Invalid URL format"}), 400
        
        # Sanitize the URL input
        sanitized_url = sanitize_url(url)
        
        # Use subprocess with proper arguments to prevent command injection
        result = subprocess.run(
            ['ping', '-c', '3', sanitized_url],
            capture_output=True,
            text=True,
            check=True
        )
        
        return jsonify({"status": "success", "result": result.stdout}), 200
    
    except subprocess.CalledProcessError as e:
        # Handle ping failures gracefully
        return jsonify({"status": "error", "message": f"Failed to reach {sanitized_url}"}), 503
    except Exception as e:
        # General error handling
        return jsonify({"status": "error", "message": str(e)}), 500

