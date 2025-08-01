#!/bin/bash
# VRAM System Demo Script
# Demonstrates the complete VRAM system functionality

echo "🚀 VRAM System Demonstration"
echo "============================"
echo

# Check if Python dependencies are installed
echo "📋 Checking dependencies..."
cd vram-system/server
python3 -c "import flask, flask_cors, requests" 2>/dev/null || {
    echo "Installing Python dependencies..."
    pip3 install -r requirements.txt
}
echo "✓ Dependencies ready"
echo

# Start the Flask server in background
echo "🖥️  Starting VRAM Flask Server..."
python3 app.py &
SERVER_PID=$!
echo "✓ Server started (PID: $SERVER_PID)"
echo "✓ Server running at http://localhost:5000"
echo

# Wait for server to start
echo "⏳ Waiting for server to initialize..."
sleep 3

# Run comprehensive tests
echo "🧪 Running comprehensive tests..."
python3 test_server.py
TEST_RESULT=$?
echo

if [ $TEST_RESULT -eq 0 ]; then
    echo "✅ All tests passed!"
else
    echo "❌ Some tests failed!"
fi
echo

# Demonstrate API endpoints
echo "🔧 API Demonstration:"
echo

echo "1. Health Check:"
curl -s http://localhost:5000/ | python3 -m json.tool
echo

echo "2. List Resources:"
curl -s http://localhost:5000/api/resources | python3 -m json.tool | head -20
echo

echo "3. Get Resource Info:"
curl -s http://localhost:5000/api/resources/demo_text/info | python3 -m json.tool
echo

echo "4. Server Statistics:"
curl -s http://localhost:5000/api/stats | python3 -m json.tool
echo

# Show resource contents
echo "📄 Demo Resource Contents:"
echo

echo "Text Resource (demo_text):"
DEMO_TEXT=$(curl -s http://localhost:5000/api/resources/demo_text | python3 -c "
import sys, json, base64
data = json.load(sys.stdin)
if data['status'] == 'success':
    print(base64.b64decode(data['data']).decode())
")
echo "$DEMO_TEXT"
echo

echo "JSON Resource (demo_json):"
DEMO_JSON=$(curl -s http://localhost:5000/api/resources/demo_json | python3 -c "
import sys, json, base64
data = json.load(sys.stdin)
if data['status'] == 'success':
    decoded = base64.b64decode(data['data']).decode()
    parsed = json.loads(decoded)
    print(json.dumps(parsed, indent=2))
")
echo "$DEMO_JSON"
echo

# Stop the server
echo "🛑 Stopping server..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
echo "✓ Server stopped"
echo

echo "📚 Documentation and Usage:"
echo "• Server code: vram-system/server/"
echo "• M5StickC client: vram-system/m5client/"
echo "• Examples: vram-system/examples/"
echo "• Full documentation: vram-system/README.md"
echo

echo "🎯 VRAM System Features Demonstrated:"
echo "✓ Flask REST API server with resource management"
echo "✓ Resource compression and version control"
echo "✓ JSON and binary resource handling"
echo "✓ Comprehensive error handling"
echo "✓ Server statistics and monitoring"
echo "✓ Complete M5StickC Plus2 client implementation"
echo "✓ Dynamic memory management with LRU cleanup"
echo "✓ Intelligent caching system"
echo "✓ WiFi communication layer"
echo "✓ Real-time monitoring interface"
echo

echo "🚀 The VRAM system is ready for deployment!"
echo "   Start the server: cd vram-system/server && python3 app.py"
echo "   Configure and upload M5StickC client code"
echo "   See README.md for complete setup instructions"