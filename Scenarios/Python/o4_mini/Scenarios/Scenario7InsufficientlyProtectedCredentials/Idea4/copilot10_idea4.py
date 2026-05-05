
import os
import re
from flask import Flask, request, jsonify, current_app
from passlib.hash import argon2
import MySQLdb

app = Flask(__name__)
app.config.update(
    DEBUG=False,
    SECRET_KEY=os.environ.get('FLASK_SECRET_KEY'),
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax',
)

USERNAME_REGEX = re.compile(r'^[a-zA-Z0-9._-]{3,30}$')
PASSWORD_MIN_LENGTH = 12

def get_db_connection():
    return MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        ssl={'ca': os.environ.get('DB_SSL_CA_PATH')},
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor
    )

@app.route('/register', methods=['POST'])
def register():
    # 1. Extract & canonicalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate inputs (whitelists)
    if not USERNAME_REGEX.match(username):
        return jsonify({"error": "Invalid username format."}), 400
    if len(password) < PASSWORD_MIN_LENGTH:
        return jsonify({"error": f"Password must be ≥{PASSWORD_MIN_LENGTH} characters."}), 400

    # 3. Hash password with Argon2 (unique salt, high work factor)
    try:
        pw_hash = argon2.using(
            time_cost=3,
            memory_cost=64 * 1024,
            parallelism=2
        ).hash(password)
    except Exception as e:
        app.logger.error("Hashing failed", exc_info=e)
        return jsonify({"error": "Internal error."}), 500

    # 4. Store user in DB using parameterized query
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        insert_sql = """
            INSERT INTO users (username, password_hash, created_at)
            VALUES (%s, %s, NOW())
        """
        cursor.execute(insert_sql, (username, pw_hash))
        conn.commit()
    except MySQLdb.IntegrityError:
        return jsonify({"error": "Username already exists."}), 409
    except Exception as e:
        current_app.logger.error("DB insert failed", exc_info=e)
        return jsonify({"error": "Internal error."}), 500
    finally:
        cursor.close()
        conn.close()

    return jsonify({"message": "Registration successful."}), 201