/*
 * M5_Debug_Streaming.ino
 * M5StickC Plus2 Debug Streaming System
 * 
 * Real-time debug streaming via WebSocket with comprehensive monitoring
 * Author: Auto-generated for M5StickC Plus2
 * Version: 1.0.0
 */

#include <M5StickCPlus2.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>
#include <esp_system.h>
#include <esp_heap_caps.h>

// Configuration
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* websocket_server = "192.168.1.100";  // Replace with your server IP
const int websocket_port = 8765;

// WebSocket client
WebSocketsClient webSocket;

// Monitoring variables
unsigned long lastDebugSend = 0;
unsigned long lastMemoryCheck = 0;
unsigned long lastPerformanceCheck = 0;
const unsigned long DEBUG_INTERVAL = 1000;  // 1 second
const unsigned long MEMORY_INTERVAL = 2000; // 2 seconds
const unsigned long PERFORMANCE_INTERVAL = 5000; // 5 seconds

// Performance tracking
unsigned long loopCount = 0;
unsigned long startTime = 0;
float avgLoopTime = 0;

// Error tracking
int wifiReconnectAttempts = 0;
int websocketReconnectAttempts = 0;
bool systemHealthy = true;

// Function declarations
void setupWiFi();
void setupWebSocket();
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length);
void sendDebugData();
void sendMemoryInfo();
void sendPerformanceMetrics();
void sendSystemStatus();
void sendErrorLog(const String& level, const String& message);
void monitorSystem();
void handleWiFiReconnection();
void updateDisplay();

void setup() {
  // Initialize M5StickC Plus2
  M5.begin();
  M5.Lcd.setRotation(3);
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextColor(WHITE);
  M5.Lcd.setTextSize(1);
  
  Serial.begin(115200);
  delay(100);
  
  // Display startup message
  M5.Lcd.setCursor(0, 0);
  M5.Lcd.println("M5 Debug Streaming");
  M5.Lcd.println("Initializing...");
  
  Serial.println("M5StickC Plus2 Debug Streaming System");
  Serial.println("====================================");
  
  // Initialize timing
  startTime = millis();
  
  // Setup WiFi and WebSocket
  setupWiFi();
  setupWebSocket();
  
  // Send initial system status
  sendSystemStatus();
  sendErrorLog("INFO", "System initialized successfully");
  
  M5.Lcd.fillScreen(BLACK);
  updateDisplay();
}

void loop() {
  unsigned long loopStart = millis();
  loopCount++;
  
  // Update M5 components
  M5.update();
  
  // Handle WebSocket
  webSocket.loop();
  
  // Monitor system health
  monitorSystem();
  
  // Send debug data at intervals
  unsigned long currentTime = millis();
  
  if (currentTime - lastDebugSend >= DEBUG_INTERVAL) {
    sendDebugData();
    lastDebugSend = currentTime;
  }
  
  if (currentTime - lastMemoryCheck >= MEMORY_INTERVAL) {
    sendMemoryInfo();
    lastMemoryCheck = currentTime;
  }
  
  if (currentTime - lastPerformanceCheck >= PERFORMANCE_INTERVAL) {
    sendPerformanceMetrics();
    lastPerformanceCheck = currentTime;
  }
  
  // Handle button presses
  if (M5.BtnA.wasPressed()) {
    sendErrorLog("USER", "Button A pressed");
  }
  
  if (M5.BtnB.wasPressed()) {
    sendErrorLog("USER", "Button B pressed");
    // Trigger manual system status
    sendSystemStatus();
  }
  
  // Update display
  updateDisplay();
  
  // Calculate loop performance
  unsigned long loopTime = millis() - loopStart;
  avgLoopTime = (avgLoopTime * 0.9) + (loopTime * 0.1); // Moving average
  
  delay(10); // Small delay to prevent overwhelming the system
}

void setupWiFi() {
  M5.Lcd.println("Connecting WiFi...");
  Serial.println("Connecting to WiFi...");
  
  WiFi.begin(ssid, password);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    M5.Lcd.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.println("WiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    
    M5.Lcd.println();
    M5.Lcd.println("WiFi Connected!");
    M5.Lcd.print("IP: ");
    M5.Lcd.println(WiFi.localIP());
    
    systemHealthy = true;
  } else {
    Serial.println("WiFi connection failed!");
    M5.Lcd.println("WiFi Failed!");
    systemHealthy = false;
  }
  
  delay(1000);
}

void setupWebSocket() {
  M5.Lcd.println("Setup WebSocket...");
  Serial.println("Setting up WebSocket connection...");
  
  webSocket.begin(websocket_server, websocket_port, "/");
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
  
  Serial.println("WebSocket initialized");
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_DISCONNECTED:
      Serial.println("WebSocket Disconnected");
      systemHealthy = false;
      websocketReconnectAttempts++;
      break;
      
    case WStype_CONNECTED:
      Serial.printf("WebSocket Connected to: %s\n", payload);
      systemHealthy = true;
      websocketReconnectAttempts = 0;
      sendErrorLog("INFO", "WebSocket connected successfully");
      break;
      
    case WStype_TEXT:
      Serial.printf("Received: %s\n", payload);
      handleWebSocketCommand((char*)payload);
      break;
      
    case WStype_ERROR:
      Serial.println("WebSocket Error");
      sendErrorLog("ERROR", "WebSocket communication error");
      break;
      
    default:
      break;
  }
}

void handleWebSocketCommand(const char* command) {
  DynamicJsonDocument doc(1024);
  deserializeJson(doc, command);
  
  String cmd = doc["command"];
  
  if (cmd == "get_status") {
    sendSystemStatus();
  } else if (cmd == "get_memory") {
    sendMemoryInfo();
  } else if (cmd == "get_performance") {
    sendPerformanceMetrics();
  } else if (cmd == "restart") {
    sendErrorLog("INFO", "Restart command received");
    ESP.restart();
  } else {
    sendErrorLog("WARNING", "Unknown command: " + cmd);
  }
}

void sendDebugData() {
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "debug";
  doc["timestamp"] = millis();
  doc["uptime"] = millis() - startTime;
  doc["loop_count"] = loopCount;
  doc["wifi_rssi"] = WiFi.RSSI();
  doc["wifi_status"] = WiFi.status();
  doc["system_healthy"] = systemHealthy;
  
  // IMU data if available
  float accX, accY, accZ;
  float gyroX, gyroY, gyroZ;
  M5.Imu.getAccelData(&accX, &accY, &accZ);
  M5.Imu.getGyroData(&gyroX, &gyroY, &gyroZ);
  
  doc["sensors"]["accel"]["x"] = accX;
  doc["sensors"]["accel"]["y"] = accY;
  doc["sensors"]["accel"]["z"] = accZ;
  doc["sensors"]["gyro"]["x"] = gyroX;
  doc["sensors"]["gyro"]["y"] = gyroY;
  doc["sensors"]["gyro"]["z"] = gyroZ;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendMemoryInfo() {
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "memory";
  doc["timestamp"] = millis();
  
  // ESP32 memory information
  doc["heap"]["total"] = ESP.getHeapSize();
  doc["heap"]["free"] = ESP.getFreeHeap();
  doc["heap"]["used"] = ESP.getHeapSize() - ESP.getFreeHeap();
  doc["heap"]["largest_free_block"] = ESP.getMaxAllocHeap();
  doc["heap"]["min_free"] = ESP.getMinFreeHeap();
  
  // PSRAM information (if available)
  doc["psram"]["total"] = ESP.getPsramSize();
  doc["psram"]["free"] = ESP.getFreePsram();
  doc["psram"]["used"] = ESP.getPsramSize() - ESP.getFreePsram();
  
  // Flash memory
  doc["flash"]["size"] = ESP.getFlashChipSize();
  doc["flash"]["speed"] = ESP.getFlashChipSpeed();
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendPerformanceMetrics() {
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "performance";
  doc["timestamp"] = millis();
  
  // CPU and timing metrics
  doc["cpu"]["frequency"] = ESP.getCpuFreqMHz();
  doc["performance"]["avg_loop_time"] = avgLoopTime;
  doc["performance"]["loops_per_second"] = loopCount * 1000.0 / (millis() - startTime);
  doc["performance"]["uptime"] = millis() - startTime;
  
  // Temperature (if available)
  doc["system"]["temperature"] = temperatureRead();
  
  // Network performance
  doc["network"]["wifi_reconnects"] = wifiReconnectAttempts;
  doc["network"]["websocket_reconnects"] = websocketReconnectAttempts;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendSystemStatus() {
  DynamicJsonDocument doc(1024);
  
  doc["type"] = "system_status";
  doc["timestamp"] = millis();
  
  // System information
  doc["system"]["chip_model"] = ESP.getChipModel();
  doc["system"]["chip_revision"] = ESP.getChipRevision();
  doc["system"]["sdk_version"] = ESP.getSdkVersion();
  doc["system"]["healthy"] = systemHealthy;
  
  // WiFi status
  doc["wifi"]["connected"] = (WiFi.status() == WL_CONNECTED);
  doc["wifi"]["ssid"] = WiFi.SSID();
  doc["wifi"]["ip"] = WiFi.localIP().toString();
  doc["wifi"]["rssi"] = WiFi.RSSI();
  
  // WebSocket status
  doc["websocket"]["connected"] = webSocket.isConnected();
  doc["websocket"]["reconnect_attempts"] = websocketReconnectAttempts;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendErrorLog(const String& level, const String& message) {
  DynamicJsonDocument doc(512);
  
  doc["type"] = "error_log";
  doc["timestamp"] = millis();
  doc["level"] = level;
  doc["message"] = message;
  doc["uptime"] = millis() - startTime;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
  
  // Also log to serial
  Serial.printf("[%s] %s\n", level.c_str(), message.c_str());
}

void monitorSystem() {
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    systemHealthy = false;
    handleWiFiReconnection();
  }
  
  // Check memory usage
  if (ESP.getFreeHeap() < 10000) { // Less than 10KB free
    sendErrorLog("WARNING", "Low memory: " + String(ESP.getFreeHeap()) + " bytes");
  }
  
  // Check loop performance
  if (avgLoopTime > 100) { // Loop taking more than 100ms
    sendErrorLog("WARNING", "Slow loop performance: " + String(avgLoopTime) + "ms");
  }
}

void handleWiFiReconnection() {
  static unsigned long lastReconnectAttempt = 0;
  
  if (millis() - lastReconnectAttempt > 10000) { // Try every 10 seconds
    Serial.println("Attempting WiFi reconnection...");
    sendErrorLog("INFO", "Attempting WiFi reconnection");
    
    WiFi.disconnect();
    WiFi.begin(ssid, password);
    
    wifiReconnectAttempts++;
    lastReconnectAttempt = millis();
  }
}

void updateDisplay() {
  static unsigned long lastDisplayUpdate = 0;
  
  if (millis() - lastDisplayUpdate > 1000) { // Update every second
    M5.Lcd.fillScreen(BLACK);
    M5.Lcd.setCursor(0, 0);
    M5.Lcd.setTextSize(1);
    
    // Title
    M5.Lcd.setTextColor(CYAN);
    M5.Lcd.println("M5 Debug Stream");
    M5.Lcd.println("================");
    
    // WiFi status
    M5.Lcd.setTextColor(WiFi.status() == WL_CONNECTED ? GREEN : RED);
    M5.Lcd.printf("WiFi: %s\n", WiFi.status() == WL_CONNECTED ? "OK" : "FAIL");
    
    // WebSocket status
    M5.Lcd.setTextColor(webSocket.isConnected() ? GREEN : RED);
    M5.Lcd.printf("WS: %s\n", webSocket.isConnected() ? "OK" : "FAIL");
    
    // System info
    M5.Lcd.setTextColor(WHITE);
    M5.Lcd.printf("Uptime: %lus\n", (millis() - startTime) / 1000);
    M5.Lcd.printf("Free Heap: %d\n", ESP.getFreeHeap());
    M5.Lcd.printf("Loop: %.1fms\n", avgLoopTime);
    
    // Status indicator
    M5.Lcd.setTextColor(systemHealthy ? GREEN : RED);
    M5.Lcd.printf("Status: %s\n", systemHealthy ? "HEALTHY" : "ERROR");
    
    // Instructions
    M5.Lcd.setTextColor(YELLOW);
    M5.Lcd.println("A:Log B:Status");
    
    lastDisplayUpdate = millis();
  }
}