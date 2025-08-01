/*
 * advanced_diagnostics.ino
 * Advanced M5StickC Plus2 diagnostics example
 * 
 * Comprehensive diagnostics with advanced monitoring features
 * Author: Auto-generated for M5StickC Plus2
 */

#include <M5StickCPlus2.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>
#include <esp_system.h>
#include <esp_heap_caps.h>
#include <esp_task_wdt.h>

// Configuration
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* websocket_server = "192.168.1.100";
const int websocket_port = 8765;

WebSocketsClient webSocket;

// Advanced monitoring variables
struct SystemMetrics {
  unsigned long bootTime;
  unsigned long lastWDTFeed;
  float temperatureHistory[10];
  int tempIndex;
  unsigned long memoryLeakDetector;
  float cpuUsageHistory[5];
  int cpuIndex;
  unsigned long networkErrorCount;
  unsigned long sensorErrorCount;
};

SystemMetrics metrics;

// Timing intervals
const unsigned long DIAGNOSTICS_INTERVAL = 5000;  // 5 seconds
const unsigned long SENSOR_INTERVAL = 1000;       // 1 second
const unsigned long HEALTH_CHECK_INTERVAL = 10000; // 10 seconds

unsigned long lastDiagnostics = 0;
unsigned long lastSensorRead = 0;
unsigned long lastHealthCheck = 0;

void setup() {
  M5.begin();
  M5.Lcd.setRotation(3);
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextColor(WHITE);
  M5.Lcd.setTextSize(1);
  
  Serial.begin(115200);
  delay(100);
  
  // Initialize metrics
  initializeMetrics();
  
  // Setup watchdog
  esp_task_wdt_init(30, true); // 30 second timeout
  esp_task_wdt_add(NULL);
  
  M5.Lcd.setCursor(0, 0);
  M5.Lcd.println("Advanced Diagnostics");
  M5.Lcd.println("Initializing...");
  
  // Connect WiFi with advanced error handling
  setupAdvancedWiFi();
  
  // Setup WebSocket with diagnostics
  setupDiagnosticWebSocket();
  
  // Send initial comprehensive status
  sendAdvancedSystemStatus();
  
  M5.Lcd.fillScreen(BLACK);
  updateAdvancedDisplay();
}

void loop() {
  unsigned long loopStart = micros();
  
  // Feed watchdog
  esp_task_wdt_reset();
  metrics.lastWDTFeed = millis();
  
  M5.update();
  webSocket.loop();
  
  unsigned long currentTime = millis();
  
  // Advanced sensor monitoring
  if (currentTime - lastSensorRead >= SENSOR_INTERVAL) {
    performAdvancedSensorRead();
    lastSensorRead = currentTime;
  }
  
  // Comprehensive diagnostics
  if (currentTime - lastDiagnostics >= DIAGNOSTICS_INTERVAL) {
    performComprehensiveDiagnostics();
    lastDiagnostics = currentTime;
  }
  
  // Health check
  if (currentTime - lastHealthCheck >= HEALTH_CHECK_INTERVAL) {
    performSystemHealthCheck();
    lastHealthCheck = currentTime;
  }
  
  // Advanced button handling
  handleAdvancedButtons();
  
  // Update display
  updateAdvancedDisplay();
  
  // Calculate CPU usage
  unsigned long loopTime = micros() - loopStart;
  updateCPUUsage(loopTime);
  
  delay(10);
}

void initializeMetrics() {
  metrics.bootTime = millis();
  metrics.lastWDTFeed = millis();
  metrics.tempIndex = 0;
  metrics.cpuIndex = 0;
  metrics.memoryLeakDetector = ESP.getFreeHeap();
  metrics.networkErrorCount = 0;
  metrics.sensorErrorCount = 0;
  
  // Initialize arrays
  for (int i = 0; i < 10; i++) {
    metrics.temperatureHistory[i] = 0;
  }
  for (int i = 0; i < 5; i++) {
    metrics.cpuUsageHistory[i] = 0;
  }
}

void setupAdvancedWiFi() {
  M5.Lcd.println("Advanced WiFi Setup");
  
  // Set advanced WiFi parameters
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.setAutoConnect(true);
  WiFi.persistent(true);
  
  // Set power management
  WiFi.setSleep(false);
  
  // Connect with timeout
  WiFi.begin(ssid, password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    attempts++;
    M5.Lcd.print(".");
    
    if (attempts % 10 == 0) {
      sendDiagnosticLog("WARNING", "WiFi connection taking longer than expected");
    }
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    M5.Lcd.println("\nWiFi Connected!");
    sendDiagnosticLog("INFO", "WiFi connected successfully");
    
    // Get detailed WiFi info
    sendAdvancedWiFiInfo();
  } else {
    M5.Lcd.println("\nWiFi Failed!");
    sendDiagnosticLog("ERROR", "WiFi connection failed after 30 attempts");
    metrics.networkErrorCount++;
  }
}

void setupDiagnosticWebSocket() {
  M5.Lcd.println("Setup Diagnostic WS");
  
  webSocket.begin(websocket_server, websocket_port, "/");
  webSocket.onEvent(advancedWebSocketEvent);
  webSocket.setReconnectInterval(5000);
  webSocket.enableHeartbeat(15000, 3000, 2); // Enable heartbeat
}

void advancedWebSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_DISCONNECTED:
      sendDiagnosticLog("WARNING", "WebSocket disconnected");
      metrics.networkErrorCount++;
      break;
      
    case WStype_CONNECTED:
      sendDiagnosticLog("INFO", "WebSocket connected with diagnostics enabled");
      sendAdvancedSystemStatus();
      break;
      
    case WStype_TEXT:
      handleAdvancedCommand((char*)payload);
      break;
      
    case WStype_ERROR:
      sendDiagnosticLog("ERROR", "WebSocket error occurred");
      metrics.networkErrorCount++;
      break;
      
    case WStype_PING:
      sendDiagnosticLog("DEBUG", "WebSocket ping received");
      break;
      
    case WStype_PONG:
      sendDiagnosticLog("DEBUG", "WebSocket pong received");
      break;
  }
}

void performAdvancedSensorRead() {
  try {
    float accX, accY, accZ;
    float gyroX, gyroY, gyroZ;
    
    // Read IMU with error checking
    if (M5.Imu.getAccelData(&accX, &accY, &accZ) == 0 &&
        M5.Imu.getGyroData(&gyroX, &gyroY, &gyroZ) == 0) {
      
      // Send detailed sensor data
      DynamicJsonDocument doc(1024);
      doc["type"] = "advanced_sensors";
      doc["timestamp"] = millis();
      
      doc["imu"]["accel"]["x"] = accX;
      doc["imu"]["accel"]["y"] = accY;
      doc["imu"]["accel"]["z"] = accZ;
      doc["imu"]["accel"]["magnitude"] = sqrt(accX*accX + accY*accY + accZ*accZ);
      
      doc["imu"]["gyro"]["x"] = gyroX;
      doc["imu"]["gyro"]["y"] = gyroY;
      doc["imu"]["gyro"]["z"] = gyroZ;
      doc["imu"]["gyro"]["magnitude"] = sqrt(gyroX*gyroX + gyroY*gyroY + gyroZ*gyroZ);
      
      // Calculate tilt angles
      float pitch = atan2(-accX, sqrt(accY*accY + accZ*accZ)) * 180.0 / PI;
      float roll = atan2(accY, accZ) * 180.0 / PI;
      
      doc["imu"]["orientation"]["pitch"] = pitch;
      doc["imu"]["orientation"]["roll"] = roll;
      
      // Temperature monitoring
      float temp = temperatureRead();
      metrics.temperatureHistory[metrics.tempIndex] = temp;
      metrics.tempIndex = (metrics.tempIndex + 1) % 10;
      
      // Calculate temperature statistics
      float avgTemp = 0, minTemp = 999, maxTemp = -999;
      for (int i = 0; i < 10; i++) {
        avgTemp += metrics.temperatureHistory[i];
        if (metrics.temperatureHistory[i] < minTemp) minTemp = metrics.temperatureHistory[i];
        if (metrics.temperatureHistory[i] > maxTemp) maxTemp = metrics.temperatureHistory[i];
      }
      avgTemp /= 10;
      
      doc["thermal"]["current"] = temp;
      doc["thermal"]["average"] = avgTemp;
      doc["thermal"]["min"] = minTemp;
      doc["thermal"]["max"] = maxTemp;
      
      String output;
      serializeJson(doc, output);
      
      if (webSocket.isConnected()) {
        webSocket.sendTXT(output);
      }
      
    } else {
      metrics.sensorErrorCount++;
      sendDiagnosticLog("ERROR", "Failed to read IMU sensors");
    }
    
  } catch (...) {
    metrics.sensorErrorCount++;
    sendDiagnosticLog("ERROR", "Exception in sensor reading");
  }
}

void performComprehensiveDiagnostics() {
  DynamicJsonDocument doc(2048);
  
  doc["type"] = "comprehensive_diagnostics";
  doc["timestamp"] = millis();
  doc["uptime"] = millis() - metrics.bootTime;
  
  // Memory diagnostics
  size_t freeHeap = ESP.getFreeHeap();
  size_t totalHeap = ESP.getHeapSize();
  size_t minFreeHeap = ESP.getMinFreeHeap();
  size_t maxAllocHeap = ESP.getMaxAllocHeap();
  
  doc["memory"]["heap"]["free"] = freeHeap;
  doc["memory"]["heap"]["total"] = totalHeap;
  doc["memory"]["heap"]["used"] = totalHeap - freeHeap;
  doc["memory"]["heap"]["min_free"] = minFreeHeap;
  doc["memory"]["heap"]["max_alloc"] = maxAllocHeap;
  doc["memory"]["heap"]["fragmentation"] = (1.0 - (float)maxAllocHeap / freeHeap) * 100;
  
  // Memory leak detection
  if (metrics.memoryLeakDetector > freeHeap + 1000) {
    sendDiagnosticLog("WARNING", "Potential memory leak detected");
  }
  metrics.memoryLeakDetector = freeHeap;
  
  // PSRAM diagnostics
  doc["memory"]["psram"]["total"] = ESP.getPsramSize();
  doc["memory"]["psram"]["free"] = ESP.getFreePsram();
  doc["memory"]["psram"]["used"] = ESP.getPsramSize() - ESP.getFreePsram();
  
  // Flash memory
  doc["storage"]["flash"]["size"] = ESP.getFlashChipSize();
  doc["storage"]["flash"]["speed"] = ESP.getFlashChipSpeed();
  doc["storage"]["flash"]["mode"] = ESP.getFlashChipMode();
  
  // CPU diagnostics
  doc["cpu"]["frequency"] = ESP.getCpuFreqMHz();
  doc["cpu"]["chip_revision"] = ESP.getChipRevision();
  doc["cpu"]["chip_model"] = ESP.getChipModel();
  
  // Calculate average CPU usage
  float avgCPU = 0;
  for (int i = 0; i < 5; i++) {
    avgCPU += metrics.cpuUsageHistory[i];
  }
  avgCPU /= 5;
  doc["cpu"]["usage_percent"] = avgCPU;
  
  // Network diagnostics
  doc["network"]["wifi"]["connected"] = (WiFi.status() == WL_CONNECTED);
  doc["network"]["wifi"]["rssi"] = WiFi.RSSI();
  doc["network"]["wifi"]["channel"] = WiFi.channel();
  doc["network"]["wifi"]["bssid"] = WiFi.BSSIDstr();
  doc["network"]["errors"]["total"] = metrics.networkErrorCount;
  doc["network"]["errors"]["sensor"] = metrics.sensorErrorCount;
  
  // Watchdog status
  doc["watchdog"]["last_feed"] = metrics.lastWDTFeed;
  doc["watchdog"]["uptime_since_feed"] = millis() - metrics.lastWDTFeed;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void performSystemHealthCheck() {
  bool healthy = true;
  String issues = "";
  
  // Check memory health
  if (ESP.getFreeHeap() < 15000) {
    healthy = false;
    issues += "Low memory; ";
  }
  
  // Check temperature
  float temp = temperatureRead();
  if (temp > 85) {
    healthy = false;
    issues += "High temperature; ";
  }
  
  // Check WiFi
  if (WiFi.status() != WL_CONNECTED) {
    healthy = false;
    issues += "WiFi disconnected; ";
  }
  
  // Check WebSocket
  if (!webSocket.isConnected()) {
    healthy = false;
    issues += "WebSocket disconnected; ";
  }
  
  // Check error rates
  if (metrics.networkErrorCount > 10 || metrics.sensorErrorCount > 5) {
    healthy = false;
    issues += "High error rate; ";
  }
  
  DynamicJsonDocument doc(512);
  doc["type"] = "health_check";
  doc["timestamp"] = millis();
  doc["healthy"] = healthy;
  doc["issues"] = issues;
  doc["severity"] = healthy ? "INFO" : "WARNING";
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
  
  if (!healthy) {
    sendDiagnosticLog("WARNING", "System health check failed: " + issues);
  }
}

void handleAdvancedCommand(const char* command) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, command);
  
  String cmd = doc["command"];
  
  if (cmd == "get_diagnostics") {
    performComprehensiveDiagnostics();
  } else if (cmd == "get_health") {
    performSystemHealthCheck();
  } else if (cmd == "reset_counters") {
    metrics.networkErrorCount = 0;
    metrics.sensorErrorCount = 0;
    sendDiagnosticLog("INFO", "Error counters reset");
  } else if (cmd == "force_gc") {
    // Force garbage collection (if applicable)
    sendDiagnosticLog("INFO", "Garbage collection requested");
  } else if (cmd == "emergency_restart") {
    sendDiagnosticLog("CRITICAL", "Emergency restart initiated");
    delay(100);
    ESP.restart();
  }
}

void updateCPUUsage(unsigned long loopTime) {
  // Simple CPU usage calculation based on loop time
  float usage = (float)loopTime / 10000.0 * 100.0; // Assume 10ms target
  if (usage > 100) usage = 100;
  
  metrics.cpuUsageHistory[metrics.cpuIndex] = usage;
  metrics.cpuIndex = (metrics.cpuIndex + 1) % 5;
}

void sendDiagnosticLog(const String& level, const String& message) {
  DynamicJsonDocument doc(512);
  
  doc["type"] = "diagnostic_log";
  doc["timestamp"] = millis();
  doc["level"] = level;
  doc["message"] = message;
  doc["source"] = "advanced_diagnostics";
  doc["uptime"] = millis() - metrics.bootTime;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
  
  Serial.printf("[DIAG-%s] %s\n", level.c_str(), message.c_str());
}

void sendAdvancedSystemStatus() {
  // Implementation similar to main file but with advanced metrics
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "advanced_system_status";
  doc["timestamp"] = millis();
  doc["mode"] = "advanced_diagnostics";
  doc["boot_time"] = metrics.bootTime;
  doc["error_counts"]["network"] = metrics.networkErrorCount;
  doc["error_counts"]["sensor"] = metrics.sensorErrorCount;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendAdvancedWiFiInfo() {
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "advanced_wifi_info";
  doc["timestamp"] = millis();
  doc["ssid"] = WiFi.SSID();
  doc["bssid"] = WiFi.BSSIDstr();
  doc["channel"] = WiFi.channel();
  doc["rssi"] = WiFi.RSSI();
  doc["ip"] = WiFi.localIP().toString();
  doc["gateway"] = WiFi.gatewayIP().toString();
  doc["subnet"] = WiFi.subnetMask().toString();
  doc["dns"] = WiFi.dnsIP().toString();
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void handleAdvancedButtons() {
  if (M5.BtnA.wasPressed()) {
    sendDiagnosticLog("USER", "Button A - Manual diagnostics trigger");
    performComprehensiveDiagnostics();
  }
  
  if (M5.BtnB.wasPressed()) {
    sendDiagnosticLog("USER", "Button B - Health check trigger");
    performSystemHealthCheck();
  }
  
  // Long press for emergency functions
  if (M5.BtnA.pressedFor(3000)) {
    sendDiagnosticLog("CRITICAL", "Emergency restart requested via button");
    ESP.restart();
  }
}

void updateAdvancedDisplay() {
  static unsigned long lastUpdate = 0;
  
  if (millis() - lastUpdate > 1000) {
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 0);
    M5.Lcd.setTextSize(1);
    
    // Header
    M5.Lcd.setTextColor(MAGENTA);
    M5.Lcd.println("Advanced Diagnostics");
    M5.Lcd.println("===================");
    
    // Status indicators
    M5.Lcd.setTextColor(WiFi.status() == WL_CONNECTED ? GREEN : RED);
    M5.Lcd.printf("WiFi: %s\n", WiFi.status() == WL_CONNECTED ? "OK" : "FAIL");
    
    M5.Lcd.setTextColor(webSocket.isConnected() ? GREEN : RED);
    M5.Lcd.printf("WS: %s\n", webSocket.isConnected() ? "OK" : "FAIL");
    
    // Advanced metrics
    M5.Lcd.setTextColor(WHITE);
    M5.Lcd.printf("Uptime: %lus\n", (millis() - metrics.bootTime) / 1000);
    M5.Lcd.printf("Free: %d bytes\n", ESP.getFreeHeap());
    M5.Lcd.printf("Temp: %.1fC\n", temperatureRead());
    
    // Error counts
    M5.Lcd.setTextColor(YELLOW);
    M5.Lcd.printf("Net Err: %lu\n", metrics.networkErrorCount);
    M5.Lcd.printf("Sen Err: %lu\n", metrics.sensorErrorCount);
    
    // Instructions
    M5.Lcd.setTextColor(CYAN);
    M5.Lcd.println("A:Diag B:Health");
    M5.Lcd.println("Hold A: Restart");
    
    lastUpdate = millis();
  }
}