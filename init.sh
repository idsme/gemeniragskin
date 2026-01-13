#!/bin/bash

# Gemini RAG Skin - Development Environment Setup Script
# This script sets up and runs the development environment

set -e

echo "=========================================="
echo "  Gemini RAG Skin - Environment Setup"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check for Java 21
echo -e "\n${YELLOW}Checking prerequisites...${NC}"

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"
    else
        echo -e "${RED}✗ Java 21+ required, found Java $JAVA_VERSION${NC}"
        echo "Please install Java 21 or later"
        exit 1
    fi
else
    echo -e "${RED}✗ Java not found${NC}"
    echo "Please install Java 21 or later"
    exit 1
fi

# Check for Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -1 | cut -d' ' -f3)
    echo -e "${GREEN}✓ Maven $MVN_VERSION detected${NC}"
else
    echo -e "${RED}✗ Maven not found${NC}"
    echo "Please install Maven"
    exit 1
fi

# Check for GEMINI_API_KEY
if [ -z "$GEMINI_API_KEY" ]; then
    echo -e "${YELLOW}! GEMINI_API_KEY environment variable not set${NC}"
    echo "  Set it with: export GEMINI_API_KEY=your_api_key"
    echo "  The application will not work without a valid Gemini API key"
else
    echo -e "${GREEN}✓ GEMINI_API_KEY is set${NC}"
fi

# Navigate to project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "\n${YELLOW}Installing dependencies...${NC}"
mvn dependency:resolve -q

echo -e "\n${YELLOW}Building application...${NC}"
mvn clean compile -q

echo -e "\n${GREEN}=========================================="
echo "  Starting Gemini RAG Skin Application"
echo "==========================================${NC}"
echo ""
echo "Application will be available at:"
echo -e "  ${GREEN}http://localhost:8080${NC}"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Run the Spring Boot application
mvn spring-boot:run
