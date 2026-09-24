from flask import Flask, request, redirect, current_app
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Pull and sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic input validation
    # Username: 3–20 chars, letters, numbers or underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,20}', username):
        return 'Invalid username. Must be 3–20 letters, numbers, or underscores.', 400

    # Password: minimum 8 characters
    if len(password) < 8:
        return 'Password must be at least 8 characters long.', 400

    # Build DB connection from environment
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
    except KeyError as e:
        # Missing environment variable
        current_app.logger.error(f"Missing DB configuration: {e}")
        return 'Internal server error', 500
    except MySQLdb.Error as e:
        current_app.logger.error(f"Database connection error: {e}")
        return 'Internal server error', 500

    try:
        cursor = db.cursor()

        # Check for existing username
        cursor.execute(
            "SELECT id FROM users WHERE username = %s",
            (username,)
        )
        if cursor.fetchone():
            return 'Username already taken.', 400

        # Hash the password
        password_hash = generate_password_hash(password)

        # Insert the new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

    except MySQLdb.Error as e:
        # Log the detailed error, but return a generic message
        current_app.logger.error(f"Database query error: {e}")
        db.rollback()
        return 'Internal server error', 500

    finally:
        cursor.close()
        db.close()

    # On success, redirect to login (or wherever you like)
    return redirect('/login')