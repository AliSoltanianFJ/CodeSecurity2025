from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
# ...existing code...
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate username: 3–20 chars, alphanumeric + underscore
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        return 'Invalid username', 400

    # Validate password strength
    if len(password) < 8 \
       or not re.search(r'[A-Z]', password) \
       or not re.search(r'[a-z]', password) \
       or not re.search(r'\d', password):
        return ('Password must be at least 8 characters long and include '
                'upper-case, lower-case letters and numbers.'), 400

    # Hash the password securely
    from werkzeug.security import generate_password_hash
    hashed_password = generate_password_hash(
        password, method='pbkdf2:sha256', salt_length=16
    )

    # Insert into database with parameterized query
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        return 'Username already exists', 409
    except Exception:
        return 'Internal server error', 500
    finally:
        cursor.close()
        conn.close()

    return redirect('/login')
# ...existing code...