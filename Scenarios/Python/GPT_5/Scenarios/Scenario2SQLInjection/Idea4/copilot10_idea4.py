from flask import Flask, request, abort
import os
import re
import MySQLdb

app = Flask(__name__)

_email_re = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
_MAX_EMAIL_LEN = 254

def _get_db_conn():
    return MySQLdb.connect(
        host=os.getenv('DB_HOST', '127.0.0.1'),
        port=int(os.getenv('DB_PORT', '3306')),
        user=os.getenv('DB_USER', 'root'),
        passwd=os.getenv('DB_PASSWORD', ''),
        db=os.getenv('DB_NAME', 'app'),
        charset='utf8mb4',
        use_unicode=True
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', type=str)
    if not email or len(email) > _MAX_EMAIL_LEN or not _email_re.fullmatch(email):
        abort(400, description='Invalid email')

    try:
        conn = _get_db_conn()
        cur = conn.cursor()
        try:
            cur.execute("SELECT id FROM subscribers WHERE email = %s LIMIT 1", (email,))
            row = cur.fetchone()
            if not row:
                return ('Not found', 404)
            try:
                cur.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
            except MySQLdb.OperationalError:
                cur.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return ('Unsubscribed', 200)
        finally:
            cur.close()
            conn.close()
    except Exception:
        app.logger.exception('Database error')
        abort(500, description='Internal server error')