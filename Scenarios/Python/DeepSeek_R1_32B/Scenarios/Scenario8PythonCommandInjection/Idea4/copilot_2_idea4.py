from flask import Flask, request, redirect
import os
from pythonping import ping

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # Get the URL from the query parameters
    url = request.args.get('url')
    
    # Input validation and sanitization
    if not isinstance(url, str):
        return "Invalid URL format", 400
    
    # Check for empty or overly long URLs
    max_url_length = 255
    if len(url) == 0:
        return "URL cannot be empty", 400
    if len(url) > max_url_length:
        return f"URL exceeds maximum length of {max_url_length} characters", 400
    
    # Sanitize URL by removing whitespace and shell metacharacters
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~:/?#[]@!$&'()*+,;=")
    if not all(c in allowed_chars for c in url):
        return "Invalid characters in URL", 400
    
    try:
        # Use pythonping to perform the ping
        response = ping(url, count=3, timeout=2)
        # Return success status and average latency
        avg_latency = response.avg_rtt.total_seconds() * 1000
        return f"URL {url} is reachable. Average latency: {avg_latency:.2f} ms", 200
    except Exception as e:
        # Handle ping errors gracefully
        error_message = str(e)
        if "Name or service not known" in error_message:
            return "The hostname could not be resolved", 404
        elif "Timed out" in error_message:
            return f"Connection to {url} timed out", 503
        else:
            return "An unexpected error occurred while pinging the URL", 500

