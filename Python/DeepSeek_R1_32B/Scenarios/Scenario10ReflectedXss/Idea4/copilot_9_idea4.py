import flask
from flask import Flask, render_template_string
import re

app = Flask(__name__)

@app.route("/hello/<username>")
def hello(username):
    # Validate username input to prevent XSS and other attacks
    if not isinstance(username, str):
        return "Invalid username", 400
        
    # Only allow alphanumeric characters, spaces, underscores, and hyphens
    if not re.match(r'^[a-zA-Z0-9\s_-]+$', username):
        return "Invalid username", 400

    # Sanitize username by escaping special characters
    from markupsafe import escape
    safe_username = escape(username)

    # Use minimal template to prevent template injection
    response = render_template_string(
        "<p>Hello, {{ name }}!</p>",
        name=safe_username
    )

    return response

# Optional: Add error handling for unexpected inputs
@app.errorhandler(400)
def bad_request(error):
    return "Invalid request", 400

