from flask import Flask, request, redirect, render_template
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic validation
    if not username or not password:
        return "Username and password required.", 400

    # Hash the password securely
    hashed_password = generate_password_hash(password)

    # Database credentials loaded from environment variables (do not hardcode!)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_password = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_password,
            db=db_name,
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Check if username already exists
        cursor.execute("SELECT id FROM users WHERE username=%s", (username,))
        if cursor.fetchone():
            return "Username already exists.", 400

        # Insert new user into the database
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)", 
            (username, hashed_password)
        )
        db.commit()
    except Exception as e:
        # Log the detailed error internally if desired (avoid exposing details)
        # app.logger.error(f"Register Error: {e}")
        return "An error occurred while registering. Please try again later.", 500
    finally:
        cursor.close()
        db.close()

    return redirect("/login")