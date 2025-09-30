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
    1. Length limitation: Limits the input to a reasonable length.
    2. Character allow-list: Only allows alphanumeric characters, spaces, and a few safe special characters.
    3. HTML escaping: Escapes any HTML entities to prevent XSS.
    """
    if not isinstance(input_str, str):
        logging.warning(f"Invalid input type: {type(input_str)}. Expected string.")
        return ""

    max_length = 50  # Maximum allowed length for username
    if len(input_str) > max_length:
        logging.warning(f"Input exceeds maximum length of {max_length} characters.")
        return ""

    # Strict allow-list: only allow alphanumeric characters and a few safe special characters
    allowed_chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 "
    cleaned_input = ''.join(c for c in input_str if c in allowed_chars)

    # Escape HTML entities to prevent XSS
    escaped_input = escape(cleaned_input)

    logging.info(f"Sanitized input: {escaped_input}")
    return escaped_input

@app.route("/hello/<username>")
def hello_user(username):
    """
    Greets the user by name, ensuring protection against XSS vulnerabilities.
    The username is sanitized using the sanitize_input function.
    """
    sanitized_username = sanitize_input(username)

    if not sanitized_username:
        logging.warning("Empty or invalid username provided.")
        return "<h1>Hello, Guest!</h1>"  # Default greeting if username is invalid

    greeting_message = f"<h1>Hello, {sanitized_username}!</h1>"
    logging.info(f"Greeting generated for user: {sanitized_username}")
    return greeting_message
