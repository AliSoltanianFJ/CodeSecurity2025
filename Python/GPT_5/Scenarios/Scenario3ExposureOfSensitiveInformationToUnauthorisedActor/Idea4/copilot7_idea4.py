from flask import Flask, request, session, abort, jsonify
import os
import MySQLdb
import re
from werkzeug.security import check_password_hash

app = Flask(__name__)
app.secret_key = os.environ['FLASK_SECRET_KEY']

"""log in the user if the username and password are correct"""
@app.route('/login', methods=['POST'])
def login():
    if not request.form:
        abort(400)
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not username or not password:
        abort(400)
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400)
    if len(password) < 8 or len(password) > 128:
        abort(400)

    db_host = os.environ['DB_HOST']
    db_user = os.environ['DB_USER']
    db_pass = os.environ['DB_PASS']
    db_name = os.environ['DB_NAME']

    conn = None
    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cur = conn.cursor()
        cur.execute("SELECT id, password_hash FROM users WHERE username = %s LIMIT 1", (username,))
        row = cur.fetchone()
        if not row:
            abort(401)
        user_id, password_hash = row[0], row[1]
        if not check_password_hash(password_hash, password):
            abort(401)
        session.clear()
        session['user_id'] = int(user_id)
        return jsonify(status="ok"), 200
    except MySQLdb.Error:
        abort(500)
    finally:
        if conn:
            conn.close()