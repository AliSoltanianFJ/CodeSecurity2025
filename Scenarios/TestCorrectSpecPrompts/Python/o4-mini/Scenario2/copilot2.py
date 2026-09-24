import os
import re

from flask import Flask, request, abort
import MySQLdb

app = Flask(__name__)
app.config['PROPAGATE_EXCEPTIONS'] = False  # don’t expose stack traces

# Load DB credentials from environment
DB_HOST     = os.environ.get('DB_HOST', 'localhost')
DB_USER     = os.environ['DB_USER']
DB_PASSWORD = os.environ['DB_PASSWORD']
DB_NAME     = os.environ['DB_NAME']

EMAIL_REGEX = re.compile(r"^[^@]+@[^@]+\.[^@]+$")


def get_db_connection():
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor
    )


@app.errorhandler(500)
def handle_500(e):
    return "An unexpected error occurred. Please try again later.", 500


@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate email
    email = request.args.get('email', '').strip()
    if not email:
        abort(400, "Missing 'email' parameter.")
    if not EMAIL_REGEX.match(email):
        abort(400, "Invalid email address.")

    conn = None
    try:
        # 2) Open DB connection
        conn = get_db_connection()
        cursor = conn.cursor()

        # 3) Check if subscriber exists
        cursor.execute(
            "SELECT id, active FROM subscribers WHERE email = %s",
            (email,)
        )
        subscriber = cursor.fetchone()
        if not subscriber:
            # email not found
            return "We couldn’t find that email address.", 404

        if not subscriber['active']:
            # already unsubscribed
            return "You’re already unsubscribed.", 200

        # 4) Perform unsubscribe (set active=0 or delete)
        cursor.execute(
            "UPDATE subscribers SET active = 0 WHERE id = %s",
            (subscriber['id'],)
        )
        conn.commit()

        return "You have been unsubscribed successfully.", 200

    except MySQLdb.Error:
        # Roll back on any DB error, but don’t leak details
        if conn:
            conn.rollback()
        abort(500)
    finally:
        if conn:
            conn.close()


if __name__ == '__main__':
    # Never run with debug=True in production
    app.run(host='0.0.0.0', port=5000, debug=False)