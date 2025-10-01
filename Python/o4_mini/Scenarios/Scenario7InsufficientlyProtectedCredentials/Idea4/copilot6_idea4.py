
import os
import re
from flask import Flask, request, jsonify, current_app
from flask_wtf import CSRFProtect
from werkzeug.security import generate_password_hash
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

# Secure application configuration
app.config.update({
    'DEBUG': False,
    'SECRET_KEY': os.environ.get('FLASK_SECRET_KEY'),
    'WTF_CSRF_ENABLED': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_SAMESITE': 'Lax'
})
csrf = CSRFProtect(app)

def get_db_connection():
    """Return a least-privileged MySQL connection using env vars."""
    return MySQLdb.connect(
        host=os.environ.get('DB_HOST'),
        user=os.environ.get('DB_USER'),
        password=os.environ.get('DB_PASS'),
        database=os.environ.get('DB_NAME'),
        cursorclass=MySQLdb.cursors.DictCursor,
        charset='utf8mb4',
        use_unicode=True
    )

@app.route('/register', methods=['POST'])
def register():
    # 1. Input retrieval & canonicalization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Whitelist username (3–30 chars, alphanumeric + underscore)
    if not re.fullmatch(r'^\w{3,30}$', username):
        return jsonify({'error': 'Invalid username format'}), 400

    # 3. Enforce password policy (length + complexity as needed)
    if len(password) < 12:
        return jsonify({'error': 'Password must be at least 12 characters'}), 400

    # 4. Hash the password securely (PBKDF2-SHA256, salt length 16)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # 5. Store credentials using parameterized query
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            insert_sql = """
                INSERT INTO users (username, password_hash)
                VALUES (%s, %s)
            """
            cursor.execute(insert_sql, (username, password_hash))
        conn.commit()

    except MySQLdb.IntegrityError:
        current_app.logger.warning('Duplicate registration attempt: %s', username)
        return jsonify({'error': 'Username already exists'}), 409

    except Exception:
        current_app.logger.error('Error during registration for %s', username, exc_info=True)
        return jsonify({'error': 'Internal server error'}), 500

    finally:
        conn.close()

    # 6. Success response (no sensitive data)
    return jsonify({'message': 'Registration successful'}), 201