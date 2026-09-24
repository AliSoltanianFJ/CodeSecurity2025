import os
import logging

from flask import Flask, request, jsonify
import MySQLdb

app = Flask(__name__)

# === APPLICATION CONFIGURATION ===
# Turn off Flask's built-in debugger in production so stack traces are never shown to users
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# Configure basic logging (you can route this to a file or external system)
logging.basicConfig(level=logging.INFO)

def get_db_connection():
    """
    Reads credentials from environment variables; never hard-code them.
    Expects: DB_HOST, DB_USER, DB_PASS, DB_NAME to be set in the environment.
    """
    return MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4'
    )

@app.errorhandler(500)
def handle_500(e):
    # Log full exception internally
    app.logger.exception("Internal server error")
    # Generic message to the client
    return jsonify(error="Internal server error"), 500

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1) Get and validate the email parameter
    email = request.args.get('email', '').strip()
    if not email:
        return jsonify(error="Missing 'email' parameter"), 400

    # (Optional) simple email pattern check
    if '@' not in email or len(email) < 5:
        return jsonify(error="Invalid email address"), 400

    try:
        db = get_db_connection()
        cursor = db.cursor()

        # 2) Check if the user exists
        cursor.execute(
            "SELECT unsubscribed FROM newsletter_subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()

        if row is None:
            # email not found
            return jsonify(status="not_found"), 404

        unsubscribed_flag = row[0]
        if unsubscribed_flag:
            # already unsubscribed
            return jsonify(status="already_unsubscribed"), 200

        # 3) Perform the unsubscribe
        cursor.execute(
            "UPDATE newsletter_subscribers "
            "   SET unsubscribed = 1, unsubscribed_at = NOW() "
            " WHERE email = %s",
            (email,)
        )
        db.commit()

        return jsonify(status="unsubscribed"), 200

    except MySQLdb.Error as db_err:
        # Log the detailed DB error internally
        app.logger.exception("Database error during unsubscribe")
        return jsonify(error="Internal server error"), 500

    finally:
        # clean up DB resources
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

if __name__ == '__main__':
    # In production you’d use a WSGI server (gunicorn/uwsgi) instead of app.run
    app.run(host='0.0.0.0', port=8000)