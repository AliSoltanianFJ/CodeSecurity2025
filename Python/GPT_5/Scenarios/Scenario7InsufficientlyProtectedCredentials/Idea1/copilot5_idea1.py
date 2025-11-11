# ...existing code...
from flask import Flask, request, redirect, jsonify
import re
import os
import MySQLdb
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # retrieve and normalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # basic validations
    if not username or not password:
        return jsonify({'error': 'username and password are required'}), 400

    if len(username) < 3 or len(username) > 30:
        return jsonify({'error': 'username must be 3-30 characters'}), 400

    # allow only a safe subset of characters for usernames
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return jsonify({'error': 'invalid username characters'}), 400

    # enforce a minimum password length (recommend stronger policies in production)
    if len(password) < 8:
        return jsonify({'error': 'password must be at least 8 characters'}), 400

    # securely hash the password (PBKDF2-SHA256)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # load DB connection info from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'root')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cursor = conn.cursor()

        # parameterized query to avoid SQL injection
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            cursor.close()
            conn.close()
            return jsonify({'error': 'username already taken'}), 409

        cursor.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
        cursor.close()
    except MySQLdb.Error:
        # do not expose internal details
        return jsonify({'error': 'internal server error'}), 500
    finally:
        if conn:
            conn.close()

    return redirect('/login')

if __name__ == '__main__':
    # run behind a production WSGI server in real deployments
    app.run(host='0.0.0.0', port=5000, debug=False)
# ...existing code...
