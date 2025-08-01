/*
 * Basic VRAM Usage Example
 * Demonstrates simple resource loading and usage
 */

#include <M5StickCPlus2.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "../m5client/memory_manager.h"
#include "../m5client/resource_cache.h"
#include "../m5client/wifi_manager.h"

// Configuration - Update these with your settings
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char* SERVER_URL = "http://192.168.1.100:5000";

// VRAM components
MemoryManager* memoryManager;
ResourceCache* resourceCache;
WiFiManager* wifiManager;

void setup() {
    // Initialize M5StickC Plus2
    auto cfg = M5.config();
    M5.begin(cfg);
    
    Serial.begin(115200);
    Serial.println("VRAM Basic Example Starting...");
    
    // Initialize VRAM system
    memoryManager = new MemoryManager(100 * 1024); // 100KB
    resourceCache = new ResourceCache(memoryManager);
    wifiManager = new WiFiManager(WIFI_SSID, WIFI_PASSWORD, SERVER_URL);
    
    // Connect to WiFi
    if (!wifiManager->begin()) {
        Serial.println("WiFi connection failed!");
        return;
    }
    
    Serial.println("WiFi connected successfully!");
    
    // Example 1: Load a text resource
    loadAndDisplayTextResource();
    
    // Example 2: Load a JSON resource
    loadAndParseJsonResource();
    
    // Example 3: Check memory usage
    checkMemoryUsage();
    
    Serial.println("Basic example completed!");
}

void loop() {
    M5.update();
    
    // Simple status display
    M5.Display.fillScreen(BLACK);
    M5.Display.setTextColor(WHITE);
    M5.Display.setCursor(10, 20);
    M5.Display.setTextSize(1);
    M5.Display.println("VRAM Basic Example");
    M5.Display.setCursor(10, 40);
    M5.Display.print("Memory: ");
    M5.Display.print(memoryManager->getMemoryUsage(), 1);
    M5.Display.println("%");
    M5.Display.setCursor(10, 60);
    M5.Display.print("Resources: ");
    M5.Display.println(resourceCache->getCacheSize());
    
    delay(1000);
}

void loadAndDisplayTextResource() {
    Serial.println("\n=== Example 1: Loading Text Resource ===");
    
    // Try to get demo_text resource
    String resourceId = "demo_text";
    
    if (!resourceCache->hasResource(resourceId)) {
        Serial.println("Resource not in cache, loading from server...");
        
        ServerResponse response = wifiManager->getResource(resourceId);
        if (response.success) {
            // Parse and store the resource
            DynamicJsonDocument doc(2048);
            deserializeJson(doc, response.data);
            
            if (doc["status"] == "success") {
                String encodedData = doc["data"];
                String decodedData = base64::decode(encodedData);
                
                resourceCache->storeResource(
                    resourceId,
                    (const uint8_t*)decodedData.c_str(),
                    decodedData.length(),
                    1, // version
                    2  // priority
                );
                
                Serial.println("Resource loaded and cached!");
            }
        } else {
            Serial.println("Failed to load resource: " + response.error);
            return;
        }
    }
    
    // Get the resource from cache
    size_t size;
    uint8_t* data = resourceCache->getResource(resourceId, size);
    
    if (data) {
        String textContent = "";
        for (size_t i = 0; i < size; i++) {
            textContent += (char)data[i];
        }
        
        Serial.println("Text resource content: " + textContent);
    } else {
        Serial.println("Failed to get resource from cache");
    }
}

void loadAndParseJsonResource() {
    Serial.println("\n=== Example 2: Loading JSON Resource ===");
    
    String resourceId = "demo_json";
    
    if (!resourceCache->hasResource(resourceId)) {
        Serial.println("Loading JSON resource from server...");
        
        ServerResponse response = wifiManager->getResource(resourceId);
        if (response.success) {
            DynamicJsonDocument doc(2048);
            deserializeJson(doc, response.data);
            
            if (doc["status"] == "success") {
                String encodedData = doc["data"];
                String decodedData = base64::decode(encodedData);
                
                resourceCache->storeResource(
                    resourceId,
                    (const uint8_t*)decodedData.c_str(),
                    decodedData.length(),
                    1, // version
                    1  // priority
                );
                
                Serial.println("JSON resource loaded!");
            }
        } else {
            Serial.println("Failed to load JSON resource");
            return;
        }
    }
    
    // Get and parse JSON data
    size_t size;
    uint8_t* data = resourceCache->getResource(resourceId, size);
    
    if (data) {
        String jsonString = "";
        for (size_t i = 0; i < size; i++) {
            jsonString += (char)data[i];
        }
        
        DynamicJsonDocument jsonDoc(1024);
        DeserializationError error = deserializeJson(jsonDoc, jsonString);
        
        if (!error) {
            Serial.println("JSON parsed successfully:");
            Serial.println("Type: " + String((const char*)jsonDoc["type"]));
            Serial.println("Message: " + String((const char*)jsonDoc["message"]));
            
            JsonArray dataArray = jsonDoc["data"];
            Serial.print("Data array: [");
            for (size_t i = 0; i < dataArray.size(); i++) {
                Serial.print(dataArray[i].as<int>());
                if (i < dataArray.size() - 1) Serial.print(", ");
            }
            Serial.println("]");
        } else {
            Serial.println("JSON parsing failed");
        }
    }
}

void checkMemoryUsage() {
    Serial.println("\n=== Example 3: Memory Usage ===");
    
    Serial.println("Memory Statistics:");
    Serial.println("Total allocated: " + String(memoryManager->getTotalAllocated()) + " bytes");
    Serial.println("Free memory: " + String(memoryManager->getFreeMemory()) + " bytes");
    Serial.println("Memory usage: " + String(memoryManager->getMemoryUsage()) + "%");
    Serial.println("Number of blocks: " + String(memoryManager->getBlockCount()));
    
    Serial.println("\nCache Statistics:");
    Serial.println("Cache hit ratio: " + String(resourceCache->getHitRatio()) + "%");
    Serial.println("Cached resources: " + String(resourceCache->getCacheSize()));
    
    // Print detailed stats
    memoryManager->printMemoryStats();
    resourceCache->printCacheStats();
}