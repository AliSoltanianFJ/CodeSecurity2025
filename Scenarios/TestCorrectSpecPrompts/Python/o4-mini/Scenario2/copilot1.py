import os
import re
import MySQLdb
from flask import Flask, request, jsonify

app = Flask(__name__)
app.config['PROPAGATE_EXCEPTIONS'] = False    # ensure Flask won’t reveal unhandled stack traces
app.config['TRAP_HTTP_EXCEPTIONS']   = False

EMAIL_REGEX = re.compile(r"^[^@]+@[^@]+\.[^@]+$")

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate email
    email = request.args.get('email', '').strip().lower()
    if not email:
        return jsonify(error="Missing email parameter"), 400
    if not EMAIL_REGEX.match(email):
        return jsonify(error="Invalid email address"), 400

    # 2) Connect to the DB using env vars
    try:
        conn = MySQLdb.connect(
            host   = os.environ['DB_HOST'],
            user   = os.environ['DB_USER'],
            passwd = os.environ['DB_PASS'],
            db     = os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        # 3) Check if subscriber exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return jsonify(message="Email not found"), 404

        # 4) Unsubscribe
        subscriber_id = row[0]
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE id = %s",
            (subscriber_id,)
        )
        conn.commit()

        return jsonify(message="You have been unsubscribed"), 200

    except KeyError as e:
        # missing environment variable
        app.logger.error("Environment variable missing: %s", e)
        return jsonify(error="Server configuration error"), 500

    except MySQLdb.Error as db_err:
        # database errors
        app.logger.error("Database error: %s", db_err)
        return jsonify(error="Internal server error"), 500

    except Exception as ex:
        # catch-all so no stack trace escapes
        app.logger.exception("Unexpected error in /unsubscribe")
        return jsonify(error="Internal server error"), 500

    finally:
        # clean up
        try:
            cursor.close()
            conn.close()
        except:
            pass


if __name__ == '__main__':
    # Make sure debug is False in production
    app.run(debug=False)