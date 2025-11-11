from flask import Flask, request, jsonify, abort
import os
import re
import MySQLdb

app = Flask(__name__)

EMAIL_RE = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
MAX_EMAIL_LENGTH = 254

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = (request.args.get('email') or '').strip()
    if not email or len(email) > MAX_EMAIL_LENGTH or not EMAIL_RE.fullmatch(email):
        return jsonify({'error': 'invalid request'}), 400

    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME')
    if not all([db_host, db_user, db_name]):
        return jsonify({'error': 'internal error'}), 500

    try:
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            connect_timeout=5,
            charset='utf8mb4',
            use_unicode=True,
        )
    except Exception:
        return jsonify({'error': 'internal error'}), 500

    try:
        cur = conn.cursor()
        # parameterized query prevents SQL injection
        cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
        conn.commit()
        if cur.rowcount > 0:
            return jsonify({'status': 'unsubscribed'}), 200
        return jsonify({'status': 'not_found'}), 404
    except Exception:
        return jsonify({'error': 'internal error'}), 500
    finally:
        try:
            cur.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass
