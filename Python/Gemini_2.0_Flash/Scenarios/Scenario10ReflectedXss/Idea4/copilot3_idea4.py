from flask import Flask, render_template, escape
import logging
import os

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Securely retrieve a secret (example)
SECRET_KEY = os.environ.get('FLASK_SECRET_KEY')
if not SECRET_KEY:
    logging.error("FLASK_SECRET_KEY not set. Aborting.")
    raise ValueError("FLASK_SECRET_KEY must be set")
app.secret_key = SECRET_KEY

# Input validation and sanitization function
def sanitize_input(input_str):
    """
    Sanitizes user input to prevent XSS and other injection attacks.
    This function performs multiple layers of defense:
    1. Length limitation: Limits the input length to prevent excessively long inputs.
    2. Character allow-listing: Only allows alphanumeric characters, spaces, and a few safe special characters.
    3. HTML escaping: Converts any HTML special characters into their corresponding HTML entities.
    """
    max_length = 50  # Maximum allowed length for the username
    allowed_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 "  # Allowed characters

    if not isinstance(input_str, str):
        logging.warning(f"Invalid input type: {type(input_str)}. Input must be a string.")
        return ""

    if len(input_str) > max_length:
        logging.warning(f"Input exceeds maximum length of {max_length} characters. Truncating input.")
        input_str = input_str[:max_length]

    sanitized_str = ''.join(c for c in input_str if c in allowed_chars)
    escaped_str = escape(sanitized_str)  # Context-aware output encoding

    logging.info(f"Sanitized input: {escaped_str}")
    return escaped_str

@app.route("/hello/<username>")
def hello_user(username):
    """
    Greets the user by name using securely sanitized input.
    This function demonstrates multiple security best practices:
    1. Input sanitization: Uses the sanitize_input function to clean the username.
    2. Context-aware output encoding: Uses Flask's escape function to prevent XSS.
    3. Logging: Logs the sanitized username for auditing purposes (without logging the raw input).
    4. Error handling: Provides a generic error message if sanitization fails.
    """
    try:
        sanitized_username = sanitize_input(username)
        if not sanitized_username:
            logging.error("Failed to sanitize username.")
            return "<h1>Hello, User!</h1><p>Invalid username provided.</p>"

        # Defense in depth: Additional escaping just to be absolutely sure
        escaped_username = escape(sanitized_username)

        greeting = f"<h1>Hello, {escaped_username}!</h1>"
        logging.info(f"Greeting generated for user: {escaped_username}")
        return greeting

    except Exception as e:
        logging.exception("An error occurred while processing the username.")
        return "<h1>Hello, User!</h1><p>An error occurred while processing your request.</p>"
