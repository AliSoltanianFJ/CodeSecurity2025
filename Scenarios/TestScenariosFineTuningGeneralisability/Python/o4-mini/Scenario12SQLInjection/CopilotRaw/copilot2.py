from flask import Flask, request, jsonify
import os
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status      = request.args.get("status")

    # basic input validation
    if not customer_id or not status:
        return jsonify({"error": "Missing customer_id or status"}), 400

    conn    = None
    cursor  = None
    try:
        # you can pull these from env vars or config instead of hard‐coding
        conn = MySQLdb.connect(host   = "localhost",
                               user   = "root",
                               passwd = "password",
                               db     = "shop",
                               charset="utf8",
                               cursorclass=MySQLdb.cursors.DictCursor)

        cursor = conn.cursor()
        sql    = """
            SELECT *
              FROM orders
             WHERE customer_id = %s
               AND status      = %s
            ORDER BY order_date DESC
        """
        cursor.execute(sql, (customer_id, status))
        rows = cursor.fetchall()

        # return as JSON
        return jsonify({"orders": rows}), 200

    except MySQLdb.Error as e:
        app.logger.error(f"DB error fetching orders for {customer_id}/{status}: {e}")
        return jsonify({"error": "Database error"}), 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()
