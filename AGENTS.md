# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## SDK Source Reference

WeChat iLink SDK source code is at `E:\work\claw\wechat-ilink-sdk-java-main`. **Do not decompile the JAR** — read source files directly from that directory when you need to check SDK methods, models, or behavior.

Key SDK packages:
- `com.github.wechat.ilink.sdk.ILinkClient` — main client (Builder pattern)
- `com.github.wechat.ilink.sdk.core.model.*` — data models (`WeixinMessage`, `MessageItem`, `TextItem`, `ImageItem`, `VoiceItem`, `CDNMedia`, etc.) — all snake_case fields
- `com.github.wechat.ilink.sdk.core.login.*` — login flow (QR code, polling)
- `com.github.wechat.ilink.sdk.service.*` — internal service layer

## Build & Run

All commands run from `E:/work/claw/claw-assistant`:

```bash
mvn clean compile                # Compile
mvn package -DskipTests          # Package into fat JAR
mvn spring-boot:run              # Run directly via Maven
java -jar target/claw-assistant-1.0-SNAPSHOT.jar  # Run packaged JAR
```

**JVM encoding**: On Windows, add `-Dfile.encoding=UTF-8` to avoid garbled Chinese text. The app checks and warns on startup if charset isn't UTF-8. pom.xml already sets `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`.

## Configuration

Application config is in `src/main/resources/application.properties` (`.gitignore`-d — personal API keys). Copy from `application.properties.example`.

| Prefix | Module | Defaults |
|--------|--------|----------|
| `llm.*` | Text chat LLM | `apiKey`/`baseUrl` (DeepSeek) / `model=deepseek-chat` |
| `vision.*` | Multi-modal vision | `apiKey`/`baseUrl` (DashScope) / `model=qwen-vl-max` |
| `image.*` | Image generation | `apiKey`/`baseUrl` (DashScope) / `model=qwen-image-2.0` |
| `voice.*` | ASR + TTS | `apiKey`/ASR+TTS endpoints (DashScope MaaS) / `tts-enabled=true` |
| `weather.api.*` | Weather | `key`/`url` (WeatherAPI) |
| `wechat.ilink.*` | WeChat iLink | `enabled=false` / `pollIntervalMs=3000` |
| `file.parse.*` | File parsing | `maxFileSize=10MB` / `maxTextLength=30000` / `maxEmbeddedImages=5` |

Five model/service endpoints (`llm`/`vision`/`image`/`voice`/`file`) are independent. Text LLM (`LLMClient`) uses OpenAI-compatible format; vision (`VisionClient`) uses OpenAI multimodal format; image (`ImageClient`) uses DashScope multimodal-generation API; voice (`VoiceClient`, ASR+TTS) uses DashScope MaaS API.

Prompt files in `src/main/resources/prompts/`:
- `system-prompt.txt` — LLM system prompt (includes TTS capability description)
- `vision-system-prompt.txt` — vision model system prompt
- `image-system-prompt.txt` — image generation system prompt
- `voice-system-prompt.txt` — voice intent classification prompt

All prompts loaded via `PromptLoader` at `@PostConstruct` in each Client.

## Architecture

Spring Boot CLI app (`spring-boot-starter` only, no web server). Two independent subsystems run side-by-side.

### CLI Subsystem

`Main.java` starts a daemon thread at `@PostConstruct` reading stdin in a `while(true)` loop. Commands implement `CommandHandler` and auto-register via Spring DI: class name minus `"Command"` suffix is the command name.

| Command | Handler | Args |
|---------|---------|------|
| `help` | `HelpCommand` | none |
| `version` | `VersionCommand` | none |
| `status` | `StatusCommand` | none |
| `weather <city>` | `WeatherCommand` + `WeatherTool` + `WeatherConfig` | city name |

Weather uses `HttpClientUtil` → WeatherAPI.

### WeChat iLink Subsystem

Optional, gated by `wechat.ilink.enabled`. Uses `wechat-ilink-sdk:2.3.3` via `ILinkClient.builder()`.

**Flow**:
`WechatILinkClient` (Builder init → async QR login, heartbeat disabled to avoid lock conflicts with getUpdates) → `WechatMessageService` (background polling thread, wait up to 60s for login) → `MessageRouter` (routes by type then intent) → `MessageHandler` (returns `WechatReply`).

**Message routing** (`MessageRouter`):
1. `IMAGE` type → `VisionTool` directly (vision model analysis)
2. `VOICE` type → `VoiceTool` directly (ASR → intent classify → route)
3. `FILE` type → `FileTool` directly (MIME detection: image files → VisionService, documents → Tika text extraction + LLM)
4. `TEXT` type → `LLMIntentClassifier.classify()` → `Intent` (CHAT / IMAGE_GENERATE / IMAGE_ANALYZE / VOICE_REPLY) → selects handler
5. Unknown types → `SimpleReplyTool` (兜底)

**Message handlers**:
| Handler | Trigger | What it does |
|---------|---------|-------------|
| `ChatTool` | CHAT intent | ChatService → LLMClient |
| `VisionTool` | IMAGE / IMAGE_ANALYZE | Multi-level image acquisition: message CDN → context CDN → context URL → message URL, then VisionService → VisionClient |
| `ImageGenerationTool` | IMAGE_GENERATE | ImageGenerationService → ImageClient → download bytes → saves media URL to context → WechatReply.image |
| `VoiceTool` | VOICE type / VOICE_REPLY intent | ASR (prefer WeChat server result, fallback Paraformer) → classifyVoiceIntent → route (CHAT+TTS, IMAGE_GENERATE, or file reply) |
| `FileTool` | FILE type | Download CDN → Tika MIME detection → image: VisionService analysis / document: Tika text extraction + embedded image analysis → ChatService LLM analysis |
| `SimpleReplyTool` | fallback | Returns "暂时无法理解该消息类型" |

**Reply model**: All handlers return `WechatReply` (not raw String), which encapsulates four types: `TEXT` / `IMAGE` (bytes) / `VOICE` (bytes + metadata, failover to text) / `FILE` (MP3 etc.). `WechatMessageService.sendReply()` dispatches by type to SDK methods (`sendText/sendImage/sendVoice/sendFile`).

**WechatMessageService** (`pollLoop`):
- SDK v2.3.3 uses `getUpdates()` (no cursor management) and snake_case model methods
- `buildWechatMessage()` converts SDK `MessageItem` to `WechatMessage` using `getText_item()/getImage_item()/getVoice_item()/getFile_item()`
- `saveMessageToContext()` stores every message (text/voice/image/file) into `ContextStore` before routing
- `sendReply()` dispatches `WechatReply` by type

### Multi-Turn Conversation Context

New in the merged architecture: all user messages are stored in `ContextStore` for multi-turn dialogue.

`context/` package:
- `ContextStore` — interface for conversation history storage
- `Message` — record (`role`, `content`, `mediaEncryptParam`, `mediaAesKey`, `mediaUrl`), supports embedding CDN params for image/voice messages
- `InMemoryContextStore` — in-memory implementation
- `RedisContextStore` — Redis-backed implementation (for production, requires `spring-boot-starter-data-redis`)
- `RedisContextProperties` — Redis configuration

`WechatMessageService.saveMessageToContext()` stores:
- TEXT → `contextStore.append(userId, "user", text)`
- VOICE → `contextStore.append(userId, "user", "[语音]voiceText", CDN params, null)`
- IMAGE → `contextStore.append(userId, "user", "[图片]", CDN params, imageUrl)`
- FILE → `contextStore.append(userId, "user", "[文件: filename]", CDN params, null)` (marker only; full extracted text appended by FileTool after parsing)

`ChatService.chat()` → `contextStore.getHistory(userId, 20)` → `LLMClient.chat(userId, text, history)` → `contextStore.append(userId, "assistant", reply)`

`VisionHandler` finds recent image context via `contextStore.findLastByPrefix(userId, "[图片]")` for contextual image analysis.

### Voice Module (VoiceService + VoiceClient)

- **ASR**: Upload audio to DashScope file service → call Paraformer (async tasks polled with exponential backoff: 2s→4s→8s→16s→32s). 3 retries.
- **TTS**: Call CosyVoice v2 (MaaS endpoint) → get `output.audio.url` → download bytes. Parse WAV header for duration/sample rate. 3 retries, failure degrades to text.
- **Voice intent classification**: single LLM call for "meaningful check + intent" (reduces API calls). Prompt: `voice-system-prompt.txt`.

### File Parsing Module (FileParseService + FileTool)

Uses Apache Tika (AutoDetectParser) for document text extraction and MIME type detection.

**FileTool** three-way dispatch:
1. **Image files** (MIME `image/*`): download CDN → base64 data URL → VisionService multi-modal analysis → save description to context → text reply
2. **Documents** (PDF/DOCX/PPTX/XLSX/TXT): download → FileParseService.parse() → Tika text extraction + EmbeddedDocumentExtractor for embedded images → first 3 embedded images via VisionService → merged content → ChatService LLM analysis
3. **Unsupported**: return format error message

**FileParseService.parse()**:
- `AutoDetectParser` + `ToTextContentHandler` → plain text
- `EmbeddedDocumentExtractor` callback captures embedded images from OOXML packages (max `maxEmbeddedImages`)
- Text truncated at `maxTextLength` chars
- Size check at `maxFileSize` bytes
- Returns `FileParseResult(text, List<EmbeddedImage>)` or null on failure

### Agent System (emerging)

Future-proof Agent architecture bridging to `MessageRouter`. Not the active routing path.

- `AgentExecutor` → `SimpleAgentExecutor` (wraps `MessageRouter`)
- `Tool` (in `agent/tool/`) → `ChatTool` / `VisionTool` / `ImageGenerationTool` — auto-register via `ToolRegistry` at `@PostConstruct`
- `ToolRegistry` (in `agent/tool/`) — `Map<Intent, Tool>`
- `AgentContext` — userId, message, intent, rawMessage

**Planned evolution**: `SimpleAgentExecutor` → `PlannerAgentExecutor` (Planner replaces IntentClassifier) → `ReActAgentExecutor`.

### Exception Handling

- `ClawException`: checked exception for business errors (CLI, weather)
- `ImageClientException` (unchecked): API error codes like `DataInspectionFailed` (carries `errorCode` for retry discrimination)
- `VoiceClientException` (unchecked): API error codes from ASR/TTS (carries `errorCode`)

### Key Design Decisions

- Commands auto-discovered by Spring: implement `CommandHandler` + `@Component`, class name ends with `Command`.
- WeChat module optional (`enabled=false` disables without affecting CLI).
- SDK heartbeat disabled (`heartbeatEnabled=false`) because it shares a lock with `getUpdates()` causing message loss.
- SDK `executeLogin()` returns either an HTTP URL or a base64-encoded QR image; both formats are handled.
- `HttpClientUtil` is a non-Spring utility (used in `WeatherTool`).
- `ObjectMapper` unified via `JacksonConfig` `@Configuration` (ignores unknown properties, non-strict beans), reused across all Clients via DI.
- `PromptLoader` loads prompt `.txt` files from classpath; each Client calls it in `@PostConstruct`.
- `WechatReply` model centralizes reply types: text, image bytes, voice bytes (with metadata), file bytes.
- `ImageGenerationHandler` downloads image bytes from the generated URL, then saves the URL to context for later re-download.
- Context storage is write-through: every message (user text/voice/image + assistant reply) goes into `ContextStore` before routing, ensuring full conversation history.
- `ChatService` uses `ContextStore` for multi-turn dialogue (max 20 history messages); `LLMClient.buildRequestBody()` injects history messages between system prompt and current user message.
- `FileTool` delegates image analysis to `VisionService` and document analysis to `ChatService`, reusing existing infrastructure.
- Tika `MimeTypes.detect()` uses magic bytes (not file extension) for reliable MIME detection.

### File Generation Module (FileGenerationService + FileGenerationTool)

Generates PDF / DOCX files via text commands like "帮我把今天的对话总结成PDF文件".

**New Intent**: `FILE_GENERATE` (5th enum value in `Intent.java`), classified by `LLMIntentClassifier`.

**Flow**: TEXT message → `FILE_GENERATE` intent → `FileGenerationTool.handle()` → `FileGenerationService.generate()`:
1. **Format detection**: keyword matching (PDF/DOCX/文档 → format)
2. **LLM content generation**: `llmClient.chatWithSystemPrompt()` with Markdown-format prompt + conversation history
3. **File rendering**:
   - **PDF**: PDFBox 2.0.31 — `PDType0Font.load()` with classpath/system font fallback (CJK support)
   - **DOCX**: POI 5.2.5 — `XWPFDocument` with XWPFParagraph/XWPFRun (Word handles rendering)
4. **Reply**: `WechatReply.file(bytes, filename, description)` → `WechatMessageService.sendReply()` (FILE case) → `WechatILinkClient.sendFileMessage()` → SDK `client.sendFile()` async

**Dependencies**: PDFBox 2.0.31 + POI 5.2.5 (transitive from tika-parsers-standard-package). No new Maven deps.

### Agent System (emerging)
- All class-level Javadoc is in Chinese.
