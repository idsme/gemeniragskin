# Gemini RAG Skin

A lightweight, single-page RAG (Retrieval-Augmented Generation) tool for querying project documentation using Google's Gemini File Search API.

## Overview

Gemini RAG Skin allows users to:
- Upload project files (Word, PDF, TXT, MD)
- Ask solution architecture questions via predefined prompts or custom queries
- Receive grounded answers with source citations

Built entirely with Java Spring Framework with server-rendered HTML/CSS and no database required.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.x, Thymeleaf
- **Frontend**: Server-side rendered HTML/CSS with minimal JavaScript
- **Build Tool**: Maven
- **External API**: Google Gemini File Search API
- **Database**: None (stateless, prompts stored in application.properties)

## Prerequisites

- Java 21 JDK installed
- Maven installed
- Google Cloud account with Gemini API access
- `GEMINI_API_KEY` environment variable configured

## Quick Start

1. Set your Gemini API key:
   ```bash
   export GEMINI_API_KEY=your_api_key_here
   ```

2. Run the setup script:
   ```bash
   ./init.sh
   ```

3. Open your browser to [http://localhost:8080](http://localhost:8080)

## Features

### Configuration Section
- Editable system prompt
- 3 editable solution architecture prompts
- Save button persists changes to application.properties

### File Upload Section
- Multi-file upload (select multiple files at once)
- Drag-and-drop support
- Supported formats: .doc, .docx, .pdf, .txt, .md
- File size limit: 50MB per file
- Automatic validation for file type and size

### File List Section
- Display all files uploaded to current Gemini corpus
- File type icons (PDF, Word, TXT, MD)
- One-click delete functionality

### Search Section
- Custom query input field
- 3 quick-action buttons for predefined prompts
- Search disabled until files are uploaded

### Results Section
- Markdown-rendered responses
- Source citations
- Session history of all queries
- Timestamps for each query

## Design

- Minimal/clean aesthetic with auto light/dark theme (follows OS preference)
- Consistent spacing and typography
- Clear visual hierarchy

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | / | Main page |
| POST | /upload | File upload (multipart) |
| POST | /delete/{fileId} | Delete file from corpus |
| POST | /search | Submit search query |
| POST | /config/save | Save prompt configuration |

## Project Structure

```
src/main/java/com/geminiragskin/
├── corpus/          # Gemini corpus management
├── file/            # File upload/delete operations
├── search/          # Search functionality
├── config/          # Prompt configuration
├── web/             # Controllers and error handling
└── model/           # Common DTOs
```

## License

MIT License
