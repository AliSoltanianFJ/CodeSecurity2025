# ...existing code...
from flask import Flask, request, redirect, jsonify
import re
import os
from werkzeug.security import generate_password_hash
import MySQLdb

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # read inputs safely
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validation
    if not username or not password:
        return jsonify({"error": "username and password are required"}), 400

    if len(username) < 3 or len(username) > 30:
        return jsonify({"error": "username must be 3-30 characters"}), 400

    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return jsonify({"error": "invalid username characters"}), 400

    # enforce a reasonably strong minimum password length
    if len(password) < 12:
        return jsonify({"error": "password must be at least 12 characters"}), 400

    # hash the password using a strong, salted algorithm (Werkzeug PBKDF2-SHA256)
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    except Exception:
        return jsonify({"error": "internal server error"}), 500

    # load DB connection settings from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()

        # check for existing username (parameterized to prevent SQL injection)
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return jsonify({"error": "username already taken"}), 409

        # insert new user with only the password hash stored
        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()

    except MySQLdb.Error:
        # do not reveal DB internals to clients
        return jsonify({"error": "internal server error"}), 500
    finally:
        if cursor:
            try:
                cursor.close()
            except Exception:
                pass
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    return redirect('/login')

# ...existing code...
if __name__ == '__main__':
    # In production, run behind a WSGI server and do not enable debug mode.
    app.run(host='0.0.0.0', port=5000, debug=False)
