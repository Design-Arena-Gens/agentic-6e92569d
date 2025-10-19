#include <SoftwareSerial.h>

// Pin Definitions (const for safety and optimization)
const uint8_t HC05_RX_PIN = 10;
const uint8_t HC05_TX_PIN = 11;

// L293D Motor Driver Pins
// Steering Motor (M1)
const uint8_t STEERING_IN1_PIN = 2;
const uint8_t STEERING_IN2_PIN = 3;
const uint8_t STEERING_EN_PIN = 5; // PWM

// Propulsion Motor (M3)
const uint8_t PROPULSION_IN3_PIN = 4;
const uint8_t PROPULSION_IN4_PIN = 6;
const uint8_t PROPULSION_EN_PIN = 7; // PWM

// Built-in LED for diagnostics
const uint8_t LED_PIN = 13;

// Bluetooth serial communication
SoftwareSerial hc05(HC05_RX_PIN, HC05_TX_PIN);

void setup() {
  // Initialize serial for debugging (optional)
  Serial.begin(9600);
  Serial.println("RC Car Firmware Initializing...");

  // Initialize Bluetooth serial communication
  hc05.begin(9600);

  // Set motor control pins as outputs
  pinMode(STEERING_IN1_PIN, OUTPUT);
  pinMode(STEERING_IN2_PIN, OUTPUT);
  pinMode(STEERING_EN_PIN, OUTPUT);
  pinMode(PROPULSION_IN3_PIN, OUTPUT);
  pinMode(PROPULSION_IN4_PIN, OUTPUT);
  pinMode(PROPULSION_EN_PIN, OUTPUT);

  // Set LED pin as output
  pinMode(LED_PIN, OUTPUT);

  // Perform self-diagnostic boot sequence
  diagnosticBoot();

  // Ensure motors are stopped on startup
  stopAllMotors();

  Serial.println("Initialization Complete. Ready for commands.");
}

void loop() {
  // Check for incoming data from the HC-05 module
  if (hc05.available()) {
    char command = hc05.read();
    handleCommand(command);
  }
}

void handleCommand(char command) {
  // Acknowledge valid commands with a quick LED flash
  if (command == 'F' || command == 'B' || command == 'L' || command == 'R' || command == 'S') {
    acknowledgeCommand();
  }

  switch (command) {
    case 'F':
      goForward();
      break;
    case 'B':
      goBackward();
      break;
    case 'L':
      turnLeft();
      break;
    case 'R':
      turnRight();
      break;
    case 'S':
    default: // Treat any unknown character as 'S' for safety
      stopAllMotors();
      break;
  }
}

void goForward() {
  // Propulsion: Forward
  digitalWrite(PROPULSION_IN3_PIN, HIGH);
  digitalWrite(PROPULSION_IN4_PIN, LOW);
  analogWrite(PROPULSION_EN_PIN, 255);
}

void goBackward() {
  // Propulsion: Backward
  digitalWrite(PROPULSION_IN3_PIN, LOW);
  digitalWrite(PROPULSION_IN4_PIN, HIGH);
  analogWrite(PROPULSION_EN_PIN, 255);
}

void turnLeft() {
  // Steering: Left
  digitalWrite(STEERING_IN1_PIN, HIGH);
  digitalWrite(STEERING_IN2_PIN, LOW);
  analogWrite(STEERING_EN_PIN, 255);
}

void turnRight() {
  // Steering: Right
  digitalWrite(STEERING_IN1_PIN, LOW);
  digitalWrite(STEERING_IN2_PIN, HIGH);
  analogWrite(STEERING_EN_PIN, 255);
}

void stopAllMotors() {
  // Propulsion: Stop
  digitalWrite(PROPULSION_IN3_PIN, LOW);
  digitalWrite(PROPULSION_IN4_PIN, LOW);
  analogWrite(PROPULSION_EN_PIN, 0);

  // Steering: Neutral (0V)
  digitalWrite(STEERING_IN1_PIN, LOW);
  digitalWrite(STEERING_IN2_PIN, LOW);
  analogWrite(STEERING_EN_PIN, 0);
}

void acknowledgeCommand() {
  digitalWrite(LED_PIN, HIGH);
  delay(50);
  digitalWrite(LED_PIN, LOW);
}

void diagnosticBoot() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(LED_PIN, HIGH);
    delay(250);
    digitalWrite(LED_PIN, LOW);
    delay(250);
  }
}
