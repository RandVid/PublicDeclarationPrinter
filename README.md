# Kotlin Public Declaration Printer

A Kotlin command-line tool that scans Kotlin source files and prints all public declarations (functions, classes, properties, etc.) in a structured format.

## Features

- Recursively processes Kotlin source files in a directory
- Identifies and prints all public declarations
- Handles:
  - Functions (including extension functions)
  - Classes (regular, data, enum, interfaces)
  - Properties (vals and vars)
  - Objects and companion objects
  - Constructors
- Properly formats nested declarations with indentation
- Preserves type parameters and function parameters in output

## Installation

1. Clone this repository
2. Build the project:
```
./gradlew build
```
### Or
Download it from [releases](https://github.com/RandVid/PublicDeclarationPrinter/releases)

## Usage
```
java -jar ./public-declaration-printer <source-directory>
```

### Example
![](https://github.com/user-attachments/assets/8b81e536-16e4-4b55-b1e5-e4bf501ff720)
