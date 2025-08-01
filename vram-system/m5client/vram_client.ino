/*
 * VRAM Client for M5StickC Plus2
 * Advanced Virtual RAM system with dynamic resource management
 */

#include <M5StickCPlus2.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <base64.h>
#include "memory_manager.h"
#include "resource_cache.h"
#include "wifi_manager.h"

// Configuration
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char* SERVER_URL = "http://192.168.1.100:5000";  // Change to your server IP

// System components
MemoryManager* memoryManager;
ResourceCache* resourceCache;
WiFiManager* wifiManager;

// System state
bool systemInitialized = false;
uint32_t lastMemoryCheck = 0;
uint32_t lastServerSync = 0;
uint32_t memoryCheckInterval = 5000;   // Check memory every 5 seconds
uint32_t serverSyncInterval = 30000;   // Sync with server every 30 seconds

// Display state
int currentScreen = 0;
uint32_t lastScreenUpdate = 0;
uint32_t screenUpdateInterval = 1000; // Update display every second

void setup() {
    // Initialize M5StickC Plus2
    auto cfg = M5.config();
    M5.begin(cfg);
    
    M5.Display.setRotation(1);
    M5.Display.fillScreen(BLACK);
    M5.Display.setTextColor(WHITE);
    M5.Display.setTextSize(1);
    
    // Show startup screen
    showStartupScreen();
    
    Serial.begin(115200);
    Serial.println("VRAM System Starting...");
    
    // Initialize memory manager (200KB max)
    memoryManager = new MemoryManager(200 * 1024);
    memoryManager->setCleanupThreshold(0.9); // Start cleanup at 90%
    
    // Initialize resource cache
    resourceCache = new ResourceCache(memoryManager);
    
    // Initialize WiFi manager
    wifiManager = new WiFiManager(WIFI_SSID, WIFI_PASSWORD, SERVER_URL);
    wifiManager->setConnectionTimeout(10000);
    wifiManager->setRequestTimeout(5000);
    wifiManager->setMaxRetries(3);
    
    // Connect to WiFi and server
    if (wifiManager->begin()) {
        Serial.println("WiFi connected successfully");
        systemInitialized = true;
        
        // Load initial resources
        loadInitialResources();
    } else {
        Serial.println("WiFi connection failed");
        showErrorScreen("WiFi Connection Failed");
        delay(3000);
    }
    
    Serial.println("VRAM System Ready");
}

void loop() {
    M5.update();
    
    uint32_t currentTime = millis();
    
    // Handle button inputs
    handleButtonInputs();
    
    // Check WiFi connection
    if (systemInitialized) {
        wifiManager->checkAndReconnect();
    }
    
    // Periodic memory monitoring
    if (currentTime - lastMemoryCheck > memoryCheckInterval) {
        checkMemoryStatus();
        lastMemoryCheck = currentTime;
    }
    
    // Periodic server synchronization
    if (systemInitialized && currentTime - lastServerSync > serverSyncInterval) {
        syncWithServer();
        lastServerSync = currentTime;
    }
    
    // Update display
    if (currentTime - lastScreenUpdate > screenUpdateInterval) {
        updateDisplay();
        lastScreenUpdate = currentTime;
    }
    
    delay(50); // Small delay to prevent overwhelming the system
}

void showStartupScreen() {
    M5.Display.fillScreen(BLACK);
    M5.Display.setCursor(10, 20);
    M5.Display.setTextSize(2);
    M5.Display.println("VRAM");
    M5.Display.setTextSize(1);
    M5.Display.setCursor(10, 50);
    M5.Display.println("Virtual RAM System");
    M5.Display.setCursor(10, 70);
    M5.Display.println("Initializing...");
}

void showErrorScreen(const String& error) {
    M5.Display.fillScreen(RED);
    M5.Display.setTextColor(WHITE);
    M5.Display.setCursor(5, 20);
    M5.Display.setTextSize(1);
    M5.Display.println("ERROR:");
    M5.Display.setCursor(5, 40);
    M5.Display.println(error);
    M5.Display.setCursor(5, 60);
    M5.Display.println("Check config");
}

void loadInitialResources() {
    Serial.println("Loading initial resources...");
    
    // Get list of available resources from server
    ServerResponse response = wifiManager->listResources();
    
    if (response.success) {
        DynamicJsonDocument doc(4096);
        deserializeJson(doc, response.data);
        
        if (doc["status"] == "success") {
            JsonArray resources = doc["resources"];
            
            for (JsonObject resource : resources) {
                String resourceId = resource["id"];
                int priority = resource["priority"];
                
                // Load high priority resources immediately
                if (priority >= 2) {
                    loadResource(resourceId);
                }
            }
        }
    }
}

bool loadResource(const String& resourceId) {
    Serial.println("Loading resource: " + resourceId);
    
    // Check if already cached
    if (resourceCache->hasResource(resourceId)) {
        Serial.println("Resource already cached");
        return true;
    }
    
    // Get resource from server
    ServerResponse response = wifiManager->getResource(resourceId);
    
    if (!response.success) {
        Serial.println("Failed to get resource: " + response.error);
        return false;
    }
    
    // Parse response
    DynamicJsonDocument doc(8192);
    DeserializationError error = deserializeJson(doc, response.data);
    
    if (error) {
        Serial.println("JSON parsing failed");
        return false;
    }
    
    if (doc["status"] != "success") {
        Serial.println("Server returned error");
        return false;
    }
    
    // Decode base64 data
    String encodedData = doc["data"];
    String decodedData = base64::decode(encodedData);
    
    // Get metadata
    JsonObject metadata = doc["metadata"];
    uint32_t version = doc["version"] | 1;
    uint8_t priority = metadata["priority"] | 1;
    String type = metadata["type"] | "generic";
    
    // Store in cache
    bool success = resourceCache->storeResource(
        resourceId,
        (const uint8_t*)decodedData.c_str(),
        decodedData.length(),
        version,
        priority,
        type
    );
    
    if (success) {
        Serial.println("Resource cached successfully");
    } else {
        Serial.println("Failed to cache resource");
    }
    
    return success;
}

void checkMemoryStatus() {
    float memoryUsage = memoryManager->getMemoryUsage();
    
    if (memoryUsage > 90.0) {
        Serial.println("WARNING: Memory usage critical (" + String(memoryUsage) + "%)");
        memoryManager->forceCleanup();
    } else if (memoryUsage > 75.0) {
        Serial.println("Memory usage high (" + String(memoryUsage) + "%)");
    }
    
    // Print stats periodically
    static uint32_t lastStatsUpdate = 0;
    if (millis() - lastStatsUpdate > 10000) { // Every 10 seconds
        memoryManager->printMemoryStats();
        resourceCache->printCacheStats();
        lastStatsUpdate = millis();
    }
}

void syncWithServer() {
    Serial.println("Syncing with server...");
    
    // Check versions of cached resources
    std::vector<String> cachedResources = resourceCache->listCachedResources();
    
    for (const String& resourceId : cachedResources) {
        ServerResponse response = wifiManager->checkResourceVersion(resourceId);
        
        if (response.success) {
            DynamicJsonDocument doc(1024);
            deserializeJson(doc, response.data);
            
            if (doc["status"] == "success") {
                uint32_t serverVersion = doc["version"];
                
                if (!resourceCache->isResourceValid(resourceId, serverVersion)) {
                    Serial.println("Resource " + resourceId + " is outdated, reloading...");
                    resourceCache->removeResource(resourceId);
                    loadResource(resourceId);
                }
            }
        }
    }
}

void handleButtonInputs() {
    if (M5.BtnA.wasPressed()) {
        currentScreen = (currentScreen + 1) % 4; // Cycle through 4 screens
        updateDisplay();
    }
    
    if (M5.BtnB.wasPressed()) {
        // Force memory cleanup
        memoryManager->forceCleanup();
        Serial.println("Manual memory cleanup triggered");
    }
    
    if (M5.BtnPWR.wasPressed()) {
        // Power management or system info
        wifiManager->printNetworkInfo();
        memoryManager->printMemoryStats();
    }
}

void updateDisplay() {
    M5.Display.fillScreen(BLACK);
    M5.Display.setTextColor(WHITE);
    M5.Display.setTextSize(1);
    
    switch (currentScreen) {
        case 0:
            showSystemStatus();
            break;
        case 1:
            showMemoryInfo();
            break;
        case 2:
            showCacheInfo();
            break;
        case 3:
            showNetworkInfo();
            break;
    }
    
    // Show screen indicator
    M5.Display.setCursor(220, 130);
    M5.Display.print(String(currentScreen + 1) + "/4");
}

void showSystemStatus() {
    M5.Display.setCursor(5, 5);
    M5.Display.setTextSize(1);
    M5.Display.println("VRAM System Status");
    M5.Display.drawLine(0, 15, 240, 15, WHITE);
    
    M5.Display.setCursor(5, 25);
    M5.Display.print("WiFi: ");
    M5.Display.println(wifiManager->isConnected() ? "Connected" : "Disconnected");
    
    M5.Display.setCursor(5, 40);
    M5.Display.print("Memory: ");
    M5.Display.print(memoryManager->getMemoryUsage(), 1);
    M5.Display.println("%");
    
    M5.Display.setCursor(5, 55);
    M5.Display.print("Cache Hit: ");
    M5.Display.print(resourceCache->getHitRatio(), 2);
    M5.Display.println("%");
    
    M5.Display.setCursor(5, 70);
    M5.Display.print("Resources: ");
    M5.Display.println(resourceCache->getCacheSize());
    
    M5.Display.setCursor(5, 85);
    M5.Display.print("Uptime: ");
    M5.Display.print(millis() / 1000);
    M5.Display.println("s");
    
    M5.Display.setCursor(5, 110);
    M5.Display.setTextSize(1);
    M5.Display.println("A:Next B:Cleanup PWR:Info");
}

void showMemoryInfo() {
    M5.Display.setCursor(5, 5);
    M5.Display.println("Memory Information");
    M5.Display.drawLine(0, 15, 240, 15, WHITE);
    
    M5.Display.setCursor(5, 25);
    M5.Display.print("Total: ");
    M5.Display.print(memoryManager->getTotalAllocated() / 1024);
    M5.Display.println(" KB");
    
    M5.Display.setCursor(5, 40);
    M5.Display.print("Free: ");
    M5.Display.print(memoryManager->getFreeMemory() / 1024);
    M5.Display.println(" KB");
    
    M5.Display.setCursor(5, 55);
    M5.Display.print("Usage: ");
    M5.Display.print(memoryManager->getMemoryUsage(), 1);
    M5.Display.println("%");
    
    M5.Display.setCursor(5, 70);
    M5.Display.print("Blocks: ");
    M5.Display.println(memoryManager->getBlockCount());
    
    // Memory usage bar
    int barWidth = 200;
    int barHeight = 10;
    int barX = 20;
    int barY = 90;
    
    M5.Display.drawRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2, WHITE);
    
    int usedWidth = (int)(memoryManager->getMemoryUsage() / 100.0 * barWidth);
    uint16_t barColor = GREEN;
    if (memoryManager->getMemoryUsage() > 75) barColor = YELLOW;
    if (memoryManager->getMemoryUsage() > 90) barColor = RED;
    
    M5.Display.fillRect(barX, barY, usedWidth, barHeight, barColor);
}

void showCacheInfo() {
    M5.Display.setCursor(5, 5);
    M5.Display.println("Cache Information");
    M5.Display.drawLine(0, 15, 240, 15, WHITE);
    
    M5.Display.setCursor(5, 25);
    M5.Display.print("Hit Ratio: ");
    M5.Display.print(resourceCache->getHitRatio(), 2);
    M5.Display.println("%");
    
    M5.Display.setCursor(5, 40);
    M5.Display.print("Hits: ");
    M5.Display.println(resourceCache->getHitCount());
    
    M5.Display.setCursor(5, 55);
    M5.Display.print("Misses: ");
    M5.Display.println(resourceCache->getMissCount());
    
    M5.Display.setCursor(5, 70);
    M5.Display.print("Cached: ");
    M5.Display.println(resourceCache->getCacheSize());
    
    // List some cached resources
    M5.Display.setCursor(5, 90);
    M5.Display.println("Recent Resources:");
    
    std::vector<String> resources = resourceCache->listCachedResources();
    int y = 105;
    for (int i = 0; i < min(2, (int)resources.size()); i++) {
        M5.Display.setCursor(5, y);
        String name = resources[i];
        if (name.length() > 25) {
            name = name.substring(0, 22) + "...";
        }
        M5.Display.println(name);
        y += 12;
    }
}

void showNetworkInfo() {
    M5.Display.setCursor(5, 5);
    M5.Display.println("Network Information");
    M5.Display.drawLine(0, 15, 240, 15, WHITE);
    
    M5.Display.setCursor(5, 25);
    M5.Display.print("Status: ");
    M5.Display.println(wifiManager->isConnected() ? "Connected" : "Disconnected");
    
    if (wifiManager->isConnected()) {
        M5.Display.setCursor(5, 40);
        M5.Display.print("IP: ");
        String ip = wifiManager->getLocalIP();
        M5.Display.println(ip);
        
        M5.Display.setCursor(5, 55);
        M5.Display.print("Signal: ");
        M5.Display.print(wifiManager->getSignalStrength());
        M5.Display.println(" dBm");
        
        M5.Display.setCursor(5, 70);
        M5.Display.print("Server: ");
        M5.Display.println("Connected");
    } else {
        M5.Display.setCursor(5, 40);
        M5.Display.println("Not connected");
        M5.Display.setCursor(5, 55);
        M5.Display.println("Check WiFi settings");
    }
}