from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    from flask import jsonify, abort
    customer_id = request.args.get("customer_id")
    status      = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # 1) Validate inputs
    if not customer_id or not status:
        abort(400, "Missing customer_id or status")

    try:
        cust_id = int(customer_id)
    except ValueError:
        abort(400, "customer_id must be an integer")

    # 2) Connect to MySQL
    conn = MySQLdb.connect(
        host   = os.getenv("DB_HOST", "localhost"),
        user   = os.getenv("DB_USER", "root"),
        passwd = os.getenv("DB_PASSWORD", ""),
        db     = os.getenv("DB_NAME", "shop"),
        charset= "utf8mb4"
    )
    cursor = conn.cursor(MySQLdb.cursors.DictCursor)

    # 3) Run a parameterized query to avoid SQL injection
    sql = """
      SELECT order_id, product_id, quantity, status, created_at
      FROM orders
      WHERE customer_id = %s
        AND status      = %s
      ORDER BY created_at DESC
    """
    cursor.execute(sql, (cust_id, status))

    # 4) Fetch and return
    orders = cursor.fetchall()
    cursor.close()
    conn.close()

    return jsonify(orders)