# ...existing code...
from flask import Flask, request, jsonify, abort
import os
import re
import MySQLdb

app = Flask(__name__)

_email_re = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
_MAX_EMAIL_LEN = 254

def _get_db_connection():
    host = os.getenv('DB_HOST', 'localhost')
    user = os.getenv('DB_USER')
    password = os.getenv('DB_PASSWORD')
    db = os.getenv('DB_NAME')
    port = int(os.getenv('DB_PORT', '3306'))
    if not all([user, password, db]):
        raise RuntimeError('database credentials not configured')
    return MySQLdb.connect(host=host, user=user, passwd=password, db=db, port=port, charset='utf8mb4')

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', type=str)
    if not email:
        return jsonify({'error': 'missing email'}), 400
    email = email.strip()
    if len(email) == 0 or len(email) > _MAX_EMAIL_LEN or not _email_re.fullmatch(email):
        return jsonify({'error': 'invalid email'}), 400

    try:
        conn = _get_db_connection()
        try:
            cur = conn.cursor()
            cur.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s AND subscribed = 1",
                (email,)
            )
            if cur.rowcount > 0:
                conn.commit()
                return '', 204
            return jsonify({'error': 'not found'}), 404
        finally:
            try:
                cur.close()
            except Exception:
                pass
            conn.close()
    except Exception:
        app.logger.exception("database error")
        return jsonify({'error': 'server error'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=False)
