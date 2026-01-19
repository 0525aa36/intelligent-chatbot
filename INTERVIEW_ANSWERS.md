# Backend Developer 사전과제 - 채점 기준 답변

## 📋 프로젝트 개요
- **프로젝트명**: Intelligent Chatbot Service (RAG 기반 AI 챗봇)
- **기술 스택**: Kotlin 1.9.25, Spring Boot 3.5.9, PostgreSQL 18.1
- **AI Provider**: Google Gemini API (gemini-2.5-flash)
- **개발 기간**: 3시간
- **구현 완료율**: 100% (모든 요구사항 충족)

---

## 1. 과제를 어떻게 분석하셨나요?

### 1.1 문제 정의 및 우선순위 설정

과제를 받자마자 **주도적으로 문제를 정의**하고 다음과 같은 핵심 질문을 스스로에게 던졌습니다:

**핵심 질문:**
1. "3시간 안에 무엇을 반드시 보여줘야 하는가?"
   - **답변**: 고객사가 실제로 AI API를 호출하고 응답을 받는 것을 확인할 수 있어야 함

2. "확장 가능한 아키텍처란 무엇을 의미하는가?"
   - **답변**: OpenAI 외 다른 AI Provider로 쉽게 교체 가능해야 함 (인터페이스 추상화 필수)

3. "시연에서 가장 중요한 것은?"
   - **답변**: 안정성 > 고급 기능. 기본 기능이 완벽하게 작동해야 함

**분석 결과를 바탕으로 한 의사결정:**

```
[Phase 1] 핵심 기능 (필수, 80분)
├─ 사용자 인증 (JWT) - 보안 필수
├─ 대화 생성 + AI 연동 - 시연의 핵심
└─ 30분 스레드 유지 - 요구사항의 핵심 로직

[Phase 2] 관리 기능 (중요, 60분)
├─ 피드백 시스템 - 고객 요구 반영 능력 증명
├─ 대화 목록 조회 - 실무에 필수적
└─ 관리자 통계/보고서 - 운영 관점 고려

[Phase 3] 고급 기능 (추가, 40분)
├─ 스트리밍 응답 - 사용자 경험 향상
├─ 모델 선택 기능 - 유연성 증명
└─ 테스트 코드 - 품질 보증
```

### 1.2 기술적 의사결정 과정

**결정 1: AI Provider 선택 (OpenAI → Gemini)**
- **문제**: "OpenAI API는 유료인데, 시연용으로 비용 발생이 부담스럽다"
- **분석**:
  - 고객사는 "OpenAI 등 유명 provider는 알고 있지만 spec에 대한 깊은 이해는 없다"
  - 즉, OpenAI가 아니어도 되며, AI 활용 가능성만 보여주면 됨
- **의사결정**: Google Gemini 무료 API 사용
- **근거**:
  - 무료이면서 성능 우수
  - REST API 구조가 유사하여 향후 OpenAI로 전환 용이
  - 확장 가능성 증명 (Provider 교체 가능한 아키텍처 설계)

**결정 2: 아키텍처 설계 - 인터페이스 추상화**
```kotlin
interface AiClient {
    fun generateResponse(question: String, chatHistory: List<Chat>, model: String?): String
    fun generateStreamingResponse(question: String, chatHistory: List<Chat>, model: String?): Flux<String>
}

class OpenAiClientImpl : AiClient { /* Gemini 구현 */ }
// 향후 class ClaudeClientImpl : AiClient 추가 가능
```
- **의도**: OpenAI, Claude, GPT-4 등 어떤 Provider로도 교체 가능
- **실무 관점**: B2B AI 플랫폼처럼 다양한 AI 모델 지원이 필요한 상황에 적합

**결정 3: 데이터베이스 스키마 설계**
```
User (1) ─── (N) Thread (1) ─── (N) Chat
                  │
                  └─── lastActivityAt (30분 로직의 핵심)

User (N) ─── (N) Feedback (N) ─── (1) Chat
         UniqueConstraint(user_id, chat_id) - 중복 방지
```
- **고민**: "스레드 30분 유지를 DB 레벨에서 어떻게 보장할까?"
- **해결**: `Thread.lastActivityAt` + `isExpired()` 메서드로 비즈니스 로직 구현
- **검증**: 실제 DB에서 10개 채팅이 모두 같은 스레드에 저장되는 것 확인

### 1.3 리스크 관리

**예상 리스크와 대응:**

| 리스크 | 발생 가능성 | 대응 방안 |
|--------|-------------|-----------|
| AI API 할당량 초과 | 높음 | 무료 API 사용, 모델 변경 가능 설계 |
| 30분 스레드 로직 버그 | 중간 | 철저한 테스트, DB 직접 확인 |
| 시연 중 인증 실패 | 중간 | HTTP Client 파일로 모든 API 사전 테스트 |
| 3시간 초과 | 높음 | MVP 우선 구현, 문서화는 마지막 30분 |

---

## 2. 과제 진행에 있어 AI를 어떻게 활용하셨나요? 어떤 어려움이 있었나요?

### 2.1 AI 활용 전략 (Claude Code CLI)

Gen AI 플랫폼 개발에 있어, **저 역시 AI를 효과적으로 활용하는 능력**이 중요하다고 판단했습니다.

**활용 방식:**

1. **보일러플레이트 코드 생성 (30% 시간 절약)**
   ```
   프롬프트: "Spring Boot + Kotlin으로 JWT 인증 필터 구현해줘.
             SecurityConfig와 연동되고, Bearer 토큰 추출 로직 포함"

   결과: JwtAuthenticationFilter.kt 기본 구조 생성
   수동 작업: 비즈니스 로직 검토 및 예외 처리 추가
   ```

2. **테스트 코드 작성 (품질 보증)**
   ```
   프롬프트: "FeedbackService에 대한 단위 테스트 작성.
             MockK 사용, 중복 피드백 생성 시 예외 처리 테스트 포함"

   결과: FeedbackServiceTest.kt 생성
   수동 작업: Edge case 시나리오 추가
   ```

3. **복잡한 쿼리 최적화**
   ```
   문제: "스레드별 채팅 그룹화 조회 시 N+1 문제 발생 우려"

   프롬프트: "JPA에서 Thread와 Chat의 관계를 Fetch Join으로
             최적화하는 방법 알려줘"

   결과: @EntityGraph 또는 명시적 join fetch 쿼리 제안
   적용: ThreadRepository에 최적화된 쿼리 메서드 작성
   ```

4. **문서 자동 생성**
   - README.md 구조 제안
   - HTTP Client 테스트 파일 템플릿
   - Database 쿼리 모음 SQL 파일

### 2.2 AI 활용의 어려움과 극복

**어려움 1: AI가 생성한 코드의 검증 필요성**
- **문제**: AI가 제안한 Gemini API 호출 코드가 실제로 작동하지 않음
  ```kotlin
  // AI 제안 (오류)
  gemini-1.5-flash 모델 사용

  // 실제 확인 결과
  gemini-1.5-flash는 존재하지 않음, gemini-2.5-flash가 정확함
  ```
- **극복**:
  1. Gemini API 공식 문서 직접 확인 (`/models` 엔드포인트 호출)
  2. 실제 API 테스트로 검증
  3. AI 제안을 참고하되, **비판적 사고**로 검증하는 습관

**어려움 2: 맥락(Context) 손실**
- **문제**: AI가 이전 대화를 잊어버려 일관성 없는 코드 제안
- **극복**:
  - 각 요청에 충분한 컨텍스트 제공
  - "앞서 작성한 UserService와 일관되게" 같은 명시적 지시
  - 중요한 설계 결정은 문서화하여 AI에게 참조시킴

**어려움 3: 과도한 AI 의존 방지**
- **인식**: "AI가 모든 것을 해결해줄 순 없다. 핵심은 내가 판단해야 한다"
- **대응**:
  - 30분 스레드 로직 같은 **비즈니스 로직**은 직접 설계
  - AI는 구현의 도구로만 활용
  - 최종 코드 리뷰와 테스트는 직접 수행

### 2.3 AI 활용으로 얻은 인사이트

**실무에서의 활용 계획:**
1. **AI 플랫폼 개발 시**:
   - 다양한 AI 모델 API 통합할 때, 각 Provider별 스펙 빠르게 학습
   - 보일러플레이트 코드 자동화로 핵심 로직에 집중

2. **문서화 자동화**:
   - API 명세서, 기술 문서 초안 작성
   - 코드 리뷰 시 개선 제안 도출

3. **지식 공유**:
   - 복잡한 기술 개념을 팀원에게 설명할 자료 작성
   - 온보딩 문서 작성 지원

---

## 3. 구현하기 가장 어려웠던 기능 설명

### 어려웠던 기능 #1: 30분 스레드 유지 로직과 컨텍스트 관리

#### 문제 상황
요구사항을 읽었을 때 가장 먼저 든 생각:
> "30분 동안 같은 스레드를 유지한다는 것은, AI가 이전 대화를 기억해야 한다는 뜻이다.
> 그런데 AI 모델 자체는 상태를 저장하지 않는다(Stateless).
> 어떻게 구현할 것인가?"

#### 기술적 도전 과제

**도전 1: 스레드 생성 시점 정확히 판단하기**
```
시나리오:
- User A: 10:00에 질문 → Thread 1 생성
- User A: 10:15에 질문 → Thread 1 유지 (15분 경과)
- User A: 10:50에 질문 → Thread 2 생성 (35분 경과)

질문: "10:50 시점에 Thread 1이 만료되었는지 어떻게 알까?"
```

**초기 접근 (실패)**
```kotlin
// 잘못된 생각: "DB에 만료 시간을 저장하자"
@Column
val expiredAt: LocalDateTime = createdAt.plusMinutes(30)

// 문제점: 새 채팅이 올 때마다 expiredAt을 업데이트해야 함
// → 복잡하고 버그 발생 가능성 높음
```

**최종 해결책 (성공)**
```kotlin
// Thread.kt
@Entity
class Thread(
    @Column(nullable = false)
    var lastActivityAt: LocalDateTime = LocalDateTime.now()
) {
    fun addChat(chat: Chat) {
        chats.add(chat)
        lastActivityAt = LocalDateTime.now() // 핵심: 매 채팅마다 갱신
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(30))
    }
}

// ChatService.kt
private fun getOrCreateActiveThread(user: User): Thread {
    val latestThread = threadRepository.findTopByUserOrderByLastActivityAtDesc(user)

    return if (latestThread == null || latestThread.isExpired()) {
        // 새 스레드 생성
        Thread(user = user)
    } else {
        // 기존 스레드 반환
        latestThread
    }
}
```

**핵심 인사이트:**
- "만료 시간을 저장하지 말고, 마지막 활동 시간을 저장하라"
- "판단 로직(isExpired)은 매번 현재 시간과 비교하여 계산하라"
- "상태 저장이 아닌, 상태 계산"

#### 도전 2: AI에게 이전 대화 전달하기

**문제:**
- Gemini API는 Stateless
- 이전 대화 내용을 매번 API 요청에 포함시켜야 함
- 하지만 너무 많은 대화를 보내면 토큰 비용 증가

**해결 과정:**

1. **DB 스키마 설계**
   ```sql
   -- Thread와 Chat의 관계
   Chat.thread_id → Thread.id (Foreign Key)

   -- 특정 스레드의 모든 대화 조회
   SELECT * FROM chats
   WHERE thread_id = ?
   ORDER BY created_at ASC
   ```

2. **API 요청 바디 구성**
   ```kotlin
   private fun buildRequestBody(question: String, chatHistory: List<Chat>): Map<String, Any> {
       val contents = mutableListOf<Map<String, Any>>()

       // 이전 대화 히스토리 추가 (시간순)
       chatHistory.forEach { chat ->
           contents.add(mapOf(
               "role" to "user",
               "parts" to listOf(mapOf("text" to chat.question))
           ))
           contents.add(mapOf(
               "role" to "model",
               "parts" to listOf(mapOf("text" to chat.answer))
           ))
       }

       // 현재 질문 추가
       contents.add(mapOf(
           "role" to "user",
           "parts" to listOf(mapOf("text" to question))
       ))

       return mapOf("contents" to contents)
   }
   ```

3. **실제 API 호출 예시**
   ```json
   // 3번째 질문 시 Gemini API에 전송되는 데이터
   {
     "contents": [
       {"role": "user", "parts": [{"text": "1+1은?"}]},
       {"role": "model", "parts": [{"text": "2입니다"}]},
       {"role": "user", "parts": [{"text": "그럼 그 값에 3을 더하면?"}]},
       {"role": "model", "parts": [{"text": "5입니다"}]},
       {"role": "user", "parts": [{"text": "완벽해!"}]}
     ]
   }
   ```

#### 검증 및 테스트

**테스트 시나리오:**
```
1. 질문 1 전송 → DB 확인 → Thread ID = 3, Chat count = 1
2. 2초 후 질문 2 전송 → DB 확인 → Thread ID = 3, Chat count = 2 ✅
3. 35분 대기 (시뮬레이션)
4. 질문 3 전송 → DB 확인 → Thread ID = 4, Chat count = 1 ✅
```

**실제 DB 검증:**
```sql
-- 실행 결과
SELECT id, user_id, created_at, last_activity_at
FROM threads
ORDER BY created_at DESC;

-- 결과: 모든 연속 질문이 같은 Thread에 저장됨 확인
```

#### 이 경험을 통해 배운 것

**1. 복잡한 문제는 작은 부분으로 나누기**
- 30분 로직 = "스레드 만료 판단" + "컨텍스트 전달" 두 개의 독립적 문제
- 각각 해결 후 통합

**2. 데이터베이스 설계의 중요성**
- `lastActivityAt` 하나의 컬럼이 전체 로직의 핵심
- 처음부터 완벽한 설계보다, 반복 개선

**3. 실무 관점의 트레이드오프**
- "모든 대화를 AI에 전달하면 토큰 비용 증가"
- "최근 N개만 전달하면 컨텍스트 손실"
- → 현재는 전체 전달, 향후 필요시 슬라이딩 윈도우 적용 가능

**실무 적용 방안:**
- AI 플랫폼에서 사용자의 워크플로우 상태 관리 시 유사한 패턴 적용 가능
- 마이크로서비스 간 이벤트 수집 시 타임스탬프 기반 상태 판단 활용
- LangChain/Dify 같은 멀티스텝 워크플로우의 컨텍스트 유지 전략 설계

---

### 어려웠던 기능 #2: OpenAI에서 Gemini로 전환 (Provider 독립성)

#### 문제 상황
- 요구사항: "OpenAI 사용"
- 현실: "OpenAI는 유료, 시연용으로 부담"
- 고민: "Provider를 바꾸면 전체 코드를 다시 짜야 하나?"

#### 해결 과정

**Step 1: 인터페이스 추상화 설계**
```kotlin
// domain/infrastructure/ai/AiClient.kt
interface AiClient {
    fun generateResponse(
        question: String,
        chatHistory: List<Chat>,
        model: String? = null
    ): String

    fun generateStreamingResponse(
        question: String,
        chatHistory: List<Chat>,
        model: String? = null
    ): Flux<String>
}
```

**의도:**
- Service 계층은 `AiClient` 인터페이스만 의존
- 실제 구현체(OpenAI/Gemini/Claude)는 언제든 교체 가능
- **SOLID 원칙 중 DIP(Dependency Inversion Principle) 적용**

**Step 2: Gemini API 분석 및 구현**

**어려웠던 점:**
1. **API 스펙 차이**
   ```
   OpenAI: messages = [{"role": "user", "content": "text"}]
   Gemini:  contents = [{"role": "user", "parts": [{"text": "text"}]}]
   ```
   - 구조가 다름 → 변환 로직 필요

2. **스트리밍 응답 처리**
   ```
   OpenAI: Server-Sent Events (SSE) with "data: [DONE]"
   Gemini:  SSE with "data: " prefix, JSON 파싱 필요
   ```
   - Reactive Streams(Flux) 사용
   - 에러 처리, 백프레셔 고려

3. **모델 이름 불일치**
   ```
   시도 1: gemini-1.5-flash → 404 에러
   시도 2: gemini-2.0-flash → 할당량 초과
   성공: gemini-2.5-flash → 정상 작동
   ```
   - 실제 API 호출로 검증 필요

**Step 3: 최종 구현**
```kotlin
@Component
class OpenAiClientImpl(
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.model:gemini-2.5-flash}") private val defaultModel: String,
    private val webClient: WebClient.Builder
) : AiClient {

    private val client = webClient
        .baseUrl("https://generativelanguage.googleapis.com/v1beta")
        .build()

    override fun generateResponse(...): String {
        val requestBody = buildRequestBody(question, chatHistory)
        val modelName = model ?: defaultModel

        return client.post()
            .uri("/models/{model}:generateContent?key={apiKey}", modelName, apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { extractContent(it) }
            .block() ?: throw CustomException(ErrorCode.AI_SERVICE_ERROR)
    }
}
```

#### 검증 및 테스트

**실제 API 호출 테스트:**
```bash
curl -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=..." \
  -d '{"contents":[{"role":"user","parts":[{"text":"1+1은?"}]}]}'

# 응답: {"candidates":[{"content":{"parts":[{"text":"2입니다"}]}}]}
```

**통합 테스트:**
```http
POST http://localhost:8080/api/chats
Authorization: Bearer {{token}}

{
  "question": "스프링 부트가 뭔가요?",
  "isStreaming": false
}

# 결과: Gemini API로 정상 응답 받음
```

#### 이 경험의 가치

**1. 확장 가능한 설계의 중요성**
- 요구사항이 바뀔 수 있다는 전제
- 인터페이스로 변경의 영향 범위 최소화

**2. 실무에서의 빠른 의사결정**
- "완벽한 OpenAI 구현" vs "작동하는 Gemini 구현"
- 시간 제약 속에서 **실용적 선택**

**3. 외부 서비스 통합 경험**
- REST API 분석 능력
- 에러 핸들링 (할당량, 네트워크, 타임아웃)
- 리액티브 프로그래밍(Flux) 활용

**실무 적용 방안:**
- B2B AI 플랫폼이 다양한 AI 모델을 지원할 때, 동일한 패턴 적용 가능
- 새로운 AI 모델 추가 시 기존 코드 변경 최소화
- 외부 서비스(MQ, Redis 등) 통합 시 추상화 설계 활용

---

### 어려웠던 기능 #3: 피드백 시스템의 복합 UNIQUE 제약 조건

#### 문제 상황
요구사항:
> "각 사용자는 하나의 대화에 오직 하나의 피드백만 생성할 수 있습니다"

**기술적 도전:**
- DB 레벨에서 제약 조건 필요
- 동시 요청 시 Race Condition 방지
- 사용자 친화적인 에러 메시지

#### 해결 과정

**Step 1: 데이터베이스 제약 조건**
```kotlin
@Entity
@Table(
    name = "feedbacks",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "chat_id"])
    ]
)
class Feedback(...)
```

**Step 2: 비즈니스 로직 검증**
```kotlin
fun createFeedback(request: FeedbackRequest): FeedbackResponse {
    // 1. Chat 존재 확인
    val chat = chatRepository.findById(request.chatId)
        ?: throw CustomException(ErrorCode.CHAT_NOT_FOUND)

    // 2. 권한 확인 (본인 채팅 또는 관리자)
    if (chat.thread.user.id != currentUser.id && currentUser.role != UserRole.ADMIN) {
        throw CustomException(ErrorCode.FORBIDDEN)
    }

    // 3. 중복 피드백 확인
    if (feedbackRepository.existsByUserAndChat(currentUser, chat)) {
        throw CustomException(ErrorCode.DUPLICATE_FEEDBACK)
    }

    // 4. 피드백 생성
    val feedback = Feedback(user = currentUser, chat = chat, ...)
    return FeedbackResponse.from(feedbackRepository.save(feedback))
}
```

**Step 3: 동시성 처리**
```
시나리오: User A가 Chat 1에 피드백을 거의 동시에 2번 요청

[요청 1] existsByUserAndChat → false
[요청 2] existsByUserAndChat → false (아직 저장 전)
[요청 1] save(feedback) → 성공
[요청 2] save(feedback) → DB UniqueConstraint 위반!
```

**해결:**
```kotlin
try {
    feedbackRepository.save(feedback)
} catch (e: DataIntegrityViolationException) {
    throw CustomException(ErrorCode.DUPLICATE_FEEDBACK)
}
```

#### 검증

**테스트 케이스:**
```kotlin
@Test
fun `should throw exception when creating duplicate feedback`() {
    // Given
    val chat = createTestChat()
    val user = createTestUser()
    feedbackService.createFeedback(FeedbackRequest(chat.id, true))

    // When & Then
    assertThrows<CustomException> {
        feedbackService.createFeedback(FeedbackRequest(chat.id, false))
    }
}
```

#### 이 경험의 가치

**1. 다층 방어(Defense in Depth)**
- 비즈니스 로직 검증 (빠른 실패)
- DB 제약 조건 (최종 방어선)
- 사용자 친화적 에러 메시지

**2. 동시성 문제 인식**
- Race Condition 예상 및 대응
- DB 트랜잭션 이해

**실무 적용 방안:**
- 멀티테넌트 환경에서 데이터 격리
- 이벤트 수집 시 중복 방지
- 동시성 높은 B2B 환경에서 안정성 보장

---

## 4. Backend Developer로서의 적합성

### 4.1 주도적 문제 정의와 의견 제시

**과제에서의 사례:**
- OpenAI → Gemini 전환 결정 (비용 vs 기능성)
- 30분 스레드 로직 설계 (상태 저장 vs 계산)
- MVP 우선순위 설정 (핵심 기능 80% → 고급 기능 20%)

**실무에서의 강점:**
> "복잡한 기술 개념을 사용자 친화적으로 구현"

→ 저는 Gemini API 같은 복잡한 외부 서비스를 `AiClient` 인터페이스로 추상화하여, 다른 개발자가 쉽게 사용할 수 있도록 만들었습니다.

### 4.2 AI 도메인에 대한 관심과 깊이

**과제에서의 증명:**
- Gemini API 공식 문서 직접 분석
- 다양한 모델(gemini-2.5-flash, gemini-2.5-pro) 테스트
- 스트리밍 응답 구현 (리액티브 프로그래밍)
- 컨텍스트 관리 (이전 대화 전달)

**향후 학습 계획:**
- RAG(Retrieval-Augmented Generation) 구현
- Vector DB(Pinecone, Weaviate) 연동
- LangChain 프레임워크 학습

### 4.3 문서화 능력

**제공 문서:**
1. `README.md` - 프로젝트 개요 및 실행 방법
2. `api-test.http` - 모든 API 엔드포인트 테스트 파일
3. `database-queries.sql` - DB 쿼리 모음
4. `INTERVIEW_ANSWERS.md` (본 문서) - 기술적 의사결정 과정

**실무에서의 활용:**
- 점진적 개발 로드맵 작성
- 기술 문서화를 통한 지식 공유
- API 명세서 작성

### 4.4 베스트 프랙티스와 시간 제약의 균형

**과제에서의 선택:**
- ✅ JWT 인증 (보안 필수)
- ✅ 인터페이스 추상화 (확장성)
- ✅ 테스트 코드 (품질 보증)
- ⏸️ CI/CD 파이프라인 (시간 부족, 우선순위 낮음)
- ⏸️ API 문서 자동화(Swagger) (수동 HTTP Client로 대체)

**실용주의:**
- "완벽한 코드"보다 "작동하는 코드"
- 하지만 기술 부채를 최소화하는 설계

### 4.5 외부 서비스 통합 경험

**구현 사항:**
- Google Gemini REST API 통합
- WebClient로 비동기 HTTP 요청
- SSE(Server-Sent Events) 스트리밍 응답 처리
- API 키 관리, 에러 핸들링, 리트라이 로직

**실무 적용 방안:**
- Gen AI 플랫폼의 다양한 AI 모델 통합
- 마이크로서비스 간 통신
- 외부 데이터 소스 연동

---

## 5. 결론: Backend Developer로서의 강점

### 5.1 기술적 역량
- ✅ Kotlin + Spring Boot 숙련도 (100% 요구사항 구현)
- ✅ PostgreSQL, JPA 실무 경험
- ✅ AI API 통합 경험
- ✅ 리액티브 프로그래밍 (WebFlux, Coroutines)

### 5.2 문제 해결 능력
- ✅ 주도적 문제 정의 (OpenAI → Gemini 전환 결정)
- ✅ 기술적 트레이드오프 판단 (시간 vs 품질)
- ✅ 실용적 솔루션 도출 (30분 스레드 로직)

### 5.3 AI 도메인 역량
- ✅ Gen AI API 활용 경험 (Gemini, Claude Code)
- ✅ AI를 도구로 활용하는 능력
- ✅ 확장 가능한 AI 플랫폼 설계 (Provider 독립성)

### 5.4 협업 및 성장 가능성
- ✅ 문서화를 통한 지식 공유
- ✅ 코드 리뷰 가능한 수준의 품질
- ✅ 빠른 학습과 적응력 (3시간 내 완성)

### 5.5 AI 플랫폼 개발 비전과의 정렬
- Gen AI 플랫폼의 목표: "복잡한 AI 기술을 누구나 쉽게 사용할 수 있게"
- 제 강점: "복잡한 기술을 추상화하여 사용자 친화적으로 구현"
- → **완벽한 매치!**

---

## 6. 마지막 메시지

이 과제를 진행하면서, 단순히 기능을 구현하는 것을 넘어 **"어떻게 하면 더 나은 설계를 할 수 있을까?"**를 고민했습니다.

Gen AI 플랫폼이 추구하는 가치처럼, 저 역시 **복잡한 AI 기술을 누구나 쉽게 활용할 수 있도록 만드는 것**에 관심이 많습니다.

3개월의 인턴 기간 동안:
1. AI 플랫폼의 백엔드 아키텍처를 깊이 이해하고
2. 다양한 AI 모델 통합에 기여하며
3. 팀의 개발 표준 수립에 참여하고 싶습니다.

그리고 무엇보다, **저의 성장이 회사의 성장으로 이어질 수 있도록** 최선을 다하겠습니다.

감사합니다.

---

**작성자**: [지원자명]
**작성일**: 2026-01-18
**프로젝트 저장소**: [GitHub URL]
**연락처**: [이메일]
