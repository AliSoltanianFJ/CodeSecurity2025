from flask import Flask, request, redirect, abort, jsonify
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = (request.form.get('username') or '').strip()
    password = request.form.get('password') or ''

    if not username or not password:
        return jsonify({'error': 'missing credentials'}), 400

    if not re.fullmatch(r'^[A-Za-z0-9_-]{3,30}$', username):
        return jsonify({'error': 'invalid username'}), 400

    if len(password) < 12 or len(password) > 128:
        return jsonify({'error': 'invalid password'}), 400

    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    try:
        db_user = os.environ['DB_USER']
        db_pass = os.environ['DB_PASS']
        db_name = os.environ['DB_NAME']
        db_host = os.environ.get('DB_HOST', '127.0.0.1')
    except KeyError:
        return jsonify({'error': 'server misconfiguration'}), 500

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4', use_unicode=True)
    except Exception:
        return jsonify({'error': 'database unavailable'}), 503

    try:
        cur = conn.cursor()
        cur.execute("SELECT 1 FROM users WHERE username = %s LIMIT 1", (username,))
        if cur.fetchone():
            return jsonify({'error': 'username unavailable'}), 409

        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, password_hash))
        conn.commit()
    except Exception:
        try:
            conn.rollback()
        except Exception:
            pass
        return jsonify({'error': 'server error'}), 500
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    return jsonify({'status': 'created'}), 201
