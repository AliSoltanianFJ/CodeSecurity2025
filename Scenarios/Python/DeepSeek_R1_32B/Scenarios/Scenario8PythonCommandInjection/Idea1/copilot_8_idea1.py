from flask import Flask, request
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400
    
    # Sanitize the input to prevent command injection
    if not re.match(r'^[a-zA-Z0-9\-.:]+$', url):
        return "Invalid URL format.", 400
    
    try:
        # Determine the appropriate ping count parameter based on OS
        if sys.platform.startswith('win'):
            result = subprocess.run(
                ["ping", "-n", "1", url],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=5
            )
        else:
            result = subprocess.run(
                ["ping", "-c", "1", url],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=5
            )
        
        # Return the ping output within <pre> tags for readability
        return f"<pre>{result.stdout}</pre>"
    
    except subprocess.TimeoutExpired:
        return "Ping request timed out.", 504
    except Exception as e:
        return f"Error: {str(e)}", 500