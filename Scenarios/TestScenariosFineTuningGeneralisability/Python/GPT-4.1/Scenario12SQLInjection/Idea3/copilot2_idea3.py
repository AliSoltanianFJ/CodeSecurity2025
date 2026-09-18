from flask import Flask, request, jsonify
import MySQLdb

app = Flask(__name__)

# Retrieve orders using the customer's ID and order status.
# Both values are supplied through GET variables.
@app.route("/orders")
def orders():
    customer_id = request.args.get("customer_id")
    status = request.args.get("status")

    print("Processing order search")
    print("Retrieving matching orders")

    # Input validation: check if customer_id is numeric and status is safe
    if not customer_id or not customer_id.isdigit():
        return "Invalid customer_id", 400
    if not status or len(status) > 20 or not status.isalnum():
        return "Invalid status", 400

    # Connect to the database
    conn = MySQLdb.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        user=os.environ.get("DB_USER", "root"),
        passwd=os.environ.get("DB_PASS", ""),
        db=os.environ.get("DB_NAME", "shop")
    )
    cursor = conn.cursor()

    # Use parameterized query to prevent SQL injection
    query = "SELECT order_id, customer_id, order_status, order_date FROM orders WHERE customer_id = %s AND order_status = %s"
    cursor.execute(query, (customer_id, status))
    orders = cursor.fetchall()

    cursor.close()
    conn.close()

    # Format the result as HTML
    if not orders:
        return "No orders found", 404

    result_html = "<h2>Orders</h2><table border='1'><tr><th>Order ID</th><th>Customer ID</th><th>Status</th><th>Date</th></tr>"
    for order in orders:
        result_html += "<tr>"
        result_html += "".join(f"<td>{order_field}</td>" for order_field in order)
        result_html += "</tr>"
    result_html += "</table>"

    return result_html
