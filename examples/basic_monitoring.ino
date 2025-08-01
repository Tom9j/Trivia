/*
 * basic_monitoring.ino
 * Basic M5StickC Plus2 monitoring example
 * 
 * Simple example showing basic debug streaming functionality
 * Author: Auto-generated for M5StickC Plus2
 */

#include <M5StickCPlus2.h>
#include <WiFi.h>
#include <WebSocketsClient.h>
#include <ArduinoJson.h>

// Configuration
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* websocket_server = "192.168.1.100";
const int websocket_port = 8765;

WebSocketsClient webSocket;
unsigned long lastSend = 0;

void setup() {
  M5.begin();
  M5.Lcd.setRotation(3);
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setTextColor(WHITE);
  M5.Lcd.setTextSize(1);
  
  Serial.begin(115200);
  
  // Display startup
  M5.Lcd.setCursor(0, 0);
  M5.Lcd.println("Basic Monitor");
  M5.Lcd.println("Connecting...");
  
  // Connect WiFi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("WiFi connected!");
  M5.Lcd.println("WiFi OK");
  
  // Setup WebSocket
  webSocket.begin(websocket_server, websocket_port, "/");
  webSocket.onEvent(webSocketEvent);
  
  M5.Lcd.println("WebSocket OK");
  delay(1000);
}

void loop() {
  M5.update();
  webSocket.loop();
  
  // Send basic data every 2 seconds
  if (millis() - lastSend > 2000) {
    sendBasicData();
    lastSend = millis();
    updateDisplay();
  }
  
  // Button handling
  if (M5.BtnA.wasPressed()) {
    sendLogMessage("INFO", "Button A pressed");
  }
  
  delay(10);
}

void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_CONNECTED:
      Serial.println("WebSocket Connected");
      sendLogMessage("INFO", "Basic monitor started");
      break;
    case WStype_DISCONNECTED:
      Serial.println("WebSocket Disconnected");
      break;
    case WStype_TEXT:
      Serial.printf("Received: %s\n", payload);
      break;
  }
}

void sendBasicData() {
  DynamicJsonDocument doc(512);
  
  doc["type"] = "debug";
  doc["timestamp"] = millis();
  doc["uptime"] = millis();
  doc["wifi_rssi"] = WiFi.RSSI();
  doc["free_heap"] = ESP.getFreeHeap();
  doc["system_healthy"] = true;
  
  // Simple sensor reading
  float accX, accY, accZ;
  M5.Imu.getAccelData(&accX, &accY, &accZ);
  doc["sensors"]["accel"]["x"] = accX;
  doc["sensors"]["accel"]["y"] = accY;
  doc["sensors"]["accel"]["z"] = accZ;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void sendLogMessage(const String& level, const String& message) {
  DynamicJsonDocument doc(256);
  
  doc["type"] = "error_log";
  doc["timestamp"] = millis();
  doc["level"] = level;
  doc["message"] = message;
  
  String output;
  serializeJson(doc, output);
  
  if (webSocket.isConnected()) {
    webSocket.sendTXT(output);
  }
}

void updateDisplay() {
  M5.Lcd.fillScreen(BLACK);
  M5.Lcd.setCursor(0, 0);
  
  M5.Lcd.setTextColor(CYAN);
  M5.Lcd.println("Basic Monitor");
  M5.Lcd.println("=============");
  
  M5.Lcd.setTextColor(WHITE);
  M5.Lcd.printf("Uptime: %lus\n", millis() / 1000);
  M5.Lcd.printf("Free Heap: %d\n", ESP.getFreeHeap());
  M5.Lcd.printf("WiFi RSSI: %d\n", WiFi.RSSI());
  
  M5.Lcd.setTextColor(webSocket.isConnected() ? GREEN : RED);
  M5.Lcd.printf("WebSocket: %s\n", webSocket.isConnected() ? "OK" : "FAIL");
  
  M5.Lcd.setTextColor(YELLOW);
  M5.Lcd.println("A: Send Log");
}