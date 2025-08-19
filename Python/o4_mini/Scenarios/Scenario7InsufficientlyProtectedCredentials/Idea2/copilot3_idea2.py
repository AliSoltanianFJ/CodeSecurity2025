from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
# ...existing code...
    # Validate input presence
    if not username or not password:
        return "Invalid input", 400

    # Validate username: only alphanumeric and underscores, length 3-30
    if not re.fullmatch(r'^\w{3,30}$', username):
        return "Invalid username", 400

    # Validate password length
    if len(password) < 8 or len(password) > 128:
        return "Invalid password", 400

    # Hash password using PBKDF2
    from werkzeug.security import generate_password_hash
    hashed_password = generate_password_hash(password)

    # Insert into database securely
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception:
        app.logger.exception("Database error")
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass

    return redirect('/login')
# ...existing code...