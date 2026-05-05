from flask import Flask, render_template, escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    # Input validation: Check if the username contains only alphanumeric characters and is within a reasonable length.
    if not isinstance(username, str) or not username.isalnum() or len(username) > 32:
        return "Invalid username. Please use alphanumeric characters only and keep it under 32 characters.", 400  # Return a 400 Bad Request error

    # Context-aware output encoding using Flask's escape function to prevent XSS.
    escaped_username = escape(username)

    # Return the greeting with the escaped username.
    return f"Hello, {escaped_username}!"
