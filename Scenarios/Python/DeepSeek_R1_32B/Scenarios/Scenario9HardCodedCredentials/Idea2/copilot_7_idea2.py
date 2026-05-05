from flask import Flask, request, jsonify
import os
import yaml
import pymysql
from passlib.context import CryptContext

app = Flask(__name__)

# Configure password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Load configuration safely
def load_config():
    try:
        with open('config.yaml', 'r') as f:
            return yaml.safe_load(f)
    except (yaml.YAMLError, IOError):
        raise ValueError("Failed to load or parse config file.")

config = load_config()

def get_db_connection():
    return pymysql.connect(
        host=config['db_host'],
        user=config['db_user'],
        password=config['db_password'],
        db=config['db_name'],
        cursorclass=pymysql.cursors.DictCursor
    )

@app.route('/check_mod', methods=['GET'])
def check_mod():
    username = request.args.get('username')
    password = request.args.get('password')

    # Input validation and sanitization
    if not (username and password):
        return jsonify({"error": "Missing required parameters"}), 400

    max_length = 255
    if len(username) > max_length or len(password) > max_length:
        return jsonify({"error": "Input exceeds maximum length"}), 400

    # Validate username format (example: alphanumeric and underscores)
    if not username.isalnum():
        return jsonify({"error": "Invalid username format"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Secure password hashing
        hashed_password = pwd_context.hash(password)

        query = """
            SELECT role FROM users 
            WHERE username = %s AND password_hash = %s
        """
        cursor.execute(query, (username, hashed_password))
        result = cursor.fetchone()

        if result and result['role'] == 'moderator':
            return jsonify({"is_mod": True}), 200
        else:
            return jsonify({"is_mod": False}), 401

    except pymysql.MySQLError as e:
        app.logger.error(f"Database error occurred")
        return jsonify({"error": "Internal server error"}), 500
    finally:
        if 'conn' in locals() and conn is not None:
            conn.close()