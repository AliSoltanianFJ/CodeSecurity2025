from flask import Flask, request, jsonify
import re
import subprocess
import sys

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL parameter from the request
    url = request.args.get('url')
    
    # Check if the URL is provided and not empty
    if not url:
        return jsonify({'error': 'No URL provided'}), 400
    
    # Validate the input to prevent command injection and buffer overflow
    # Only allow alphanumeric, hyphens, and dots
    valid_url_pattern = re.compile(r'^[a-zA-Z0-9\-\.]+$')
    
    if not valid_url_pattern.match(url):
        return jsonify({'error': 'Invalid URL format'}), 400
    
    # Sanitize the input by stripping any invalid characters (though regex should have caught it)
    cleaned_url = re.sub(r'[^a-zA-Z0-9\-\.]', '', url)
    
    if not cleaned_url:
        return jsonify({'error': 'Sanitized URL is empty'}), 400
    
    try:
        # Use subprocess.run with arguments as a list to prevent command injection
        result = subprocess.run(['ping', '-c', '4', cleaned_url], 
                              capture_output=True, 
                              text=True)
        
        if result.returncode == 0:
            return jsonify({'status': 'success', 'message': 'Host is reachable'})
        else:
            # Parse the output to determine the exact error
            if 'Destination Host Unreachable' in result.stderr:
                status = 'unreachable'
            elif 'Operation not permitted' in result.stderr:
                status = 'permission_denied'
            else:
                status = 'unknown_error'
            
            return jsonify({'status': 'error', 'message': 'Ping failed'})
        
    except Exception as e:
        # Handle any unexpected exceptions
        app.logger.error(f"Error pinging URL")
        return jsonify({'status': 'error', 'message': 'An error occurred while checking the URL'}), 500