from flask import Flask, request, jsonify
import re
import subprocess
from flask_limiter import Limiter

app = Flask(__name__)

# Rate limiting configuration (adjust as needed)
limiter = Limiter(app=app, key_func=lambda: request.remote_addr)
limiter.limit("10 per minute")(ping)

@app.route('/url/ping')
@limiter.limit("10 per minute")
def ping():
    # Validate input URL
    url = request.args.get('url')
    
    if not url:
        return jsonify({"error": "URL is required"}), 400
    
    # Validate URL format using regular expression
    # This pattern checks for valid http/https URLs with proper domains
    url_pattern = re.compile(
        r'^(?:http)s?://'  # http:// or https://
        r'(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+'  # domain name
        r'[a-zA-Z]{2,}'  # top-level domain
        r'(?::\d{1,5})?'  # optional port number
        r'(?:/?|[/?]\S+)$', re.IGNORECASE)
    
    if not url_pattern.match(url):
        return jsonify({"error": "Invalid URL format"}), 400
        
    # Sanitize the input (URL-encode special characters)
    # Note: subprocess automatically handles shell metacharacters when using a list
    try:
        result = subprocess.run(
            ['ping', '-c', '3', url],
            capture_output=True,
            text=True,
            check=True,
            timeout=10  # Timeout after 10 seconds to prevent hangs
        )
        
        return jsonify({
            "status": "success",
            "message": "URL is reachable"
        }), 200
        
    except subprocess.CalledProcessError:
        return jsonify({
            "status": "error",
            "message": "Unable to reach the URL"
        }), 503
        
    except TimeoutError:
        return jsonify({
            "status": "error",
            "message": "Request timed out"
        }), 504
        
    except Exception as e:
        # Log unexpected errors (ensure you have logging configured)
        app.logger.error(f"Unexpected error during ping: {str(e)}")
        return jsonify({
            "status": "error",
            "message": "Internal server error"
        }), 500