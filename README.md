# Backend Developer (JVM) - Intelligent Chatbot Service
---

## 1. 과제 분석

과제를 받자마자 **주도적으로 문제를 정의**하고 다음과 같은 핵심 질문을 스스로에게 던졌습니다.


**3시간 안에 무엇을 반드시 보여줘야 하는가?**

그리고 확장 가능한 아키텍처란 무엇을 의미하는가?에 대한 깊은 고민을 시작했습니다.

### 1.1 문제 정의 및 우선순위 설정

**핵심 질문과 답변:**

1. **3시간 안에 무엇을 반드시 보여줘야 하는가?**
    - **답변**: 고객사가 AI API를 통해 실제 질문을 던지고 응답을 받는 것을 확인할 수 있어야 합니다. **AI 연동을 통한 대화 생성 기능**이 시연의 핵심입니다.
2. **확장 가능한 아키텍처란 무엇을 의미하는가?**
    - **답변**: 요구사항에 **향후 자사의 대외비 문서를 학습시키고 싶어 합니다**라는 문구가 있었습니다. 이는 AI 모델이나 Provider가 변경될 수 있음을 암시합니다. 따라서 OpenAI 외 다른 AI Provider로 쉽게 교체 가능하도록 **인터페이스 추상화**가 필수적이라고 판단했습니다.
3. **시연에서 가장 중요한 것은?**
    - **답변**: 안정성. 아무리 고급 기능이 많아도 기본 기능이 불안정하면 시연은 실패합니다. 따라서 **기본 기능의 완벽한 작동**을 최우선으로 두었습니다.

**분석 결과를 바탕으로 한 구현 우선순위:**

- **Phase 1 (핵심 기능, 60분)**:
    - 사용자 인증 (JWT): 모든 API의 기본 보안 요구사항
    - 대화 생성 + AI 연동: 시연의 핵심이자 과제의 본질
    - 30분 스레드 유지: 요구사항에 명시된 핵심 비즈니스 로직
- **Phase 2 (관리 기능, 60분)**:
    - 피드백 시스템: 고객 요구 반영 및 서비스 개선의 기반
    - 대화 목록 조회: 사용자와 관리자 모두에게 필요한 기본 기능
    - 관리자 통계/보고서: 운영 관점의 필수 기능
- **Phase 3 (고급 기능, 20분)**:
    - 스트리밍 응답: 사용자 경험 향상을 위한 고급 기능
    - 모델 선택 기능: AI Provider 유연성을 보여주는 기능
    - 테스트 코드: 코드 품질 보증 및 안정성 확보

### 1.2 기술적 의사결정 과정

- **AI Provider 선택 (OpenAI → Google Gemini)**:
    - **문제 인식**: 요구사항에 **OpenAI 등 유명 provider는 알고 있지만 spec에 대한 깊은 이해는 없다**고 명시되어 있었습니다. 또한, 시연용으로 OpenAI 유료 API를 사용하는 것은 비용 부담이 있었습니다.
    - **의사결정**: **Google Gemini 무료 API**를 사용하기로 결정했습니다.
    - **근거**: 무료이면서도 성능이 우수하여 시연 목적에 부합하고 REST API 구조가 OpenAI와 유사하여 향후 전환이 용이합니다. 이는 **확장 가능한 아키텍처**를 증명하는 좋은 사례가 됩니다.
- **아키텍처 설계 - 인터페이스 추상화 (`AiClient`)**:
    - **설계 의도**: `AiClient` 인터페이스를 정의하고 `OpenAiClientImpl` 를 구현하도록 했습니다.
    - **코드 예시**:

        ```kotlin
        interface AiClient {
            fun generateResponse(question: String, chatHistory: List<Chat>, model: String?): String
            fun generateStreamingResponse(question: String, chatHistory: List<Chat>, model: String?): Flux<String>
        }
        // 향후 class ClaudeClientImpl : AiClient 추가 가능
        
        ```
    - **실무 관점**: B2B AI 플랫폼처럼 다양한 AI 모델 지원이 필요한 상황에서 특정 Provider에 종속되지 않는 유연한 아키텍처를 제공합니다.
- **데이터베이스 스키마 설계**:
    - `User`, `Thread`, `Chat`, `Feedback` 엔티티 간의 관계를 명확히 설정했습니다. 특히 `Thread` 엔티티에 `lastActivityAt` 컬럼을 추가하여 30분 스레드 유지 로직의 핵심으로 활용했습니다.

### 1.3 리스크 관리

- **AI API 할당량 초과**: Google Gemini 무료 API를 사용하고, 모델 변경 가능 설계를 통해 대응했습니다.
- **30분 스레드 로직 버그**: `Thread.lastActivityAt` 기반의 계산 로직을 철저히 테스트하고, DB를 직접 확인하며 검증했습니다.
- **시연 중 인증 실패**: `api-test.http` 파일을 활용하여 모든 API 엔드포인트를 사전에 테스트하여 안정성을 확보했습니다.
- **2시간 초과**: MVP(Minimum Viable Product)를 우선 구현하고 문서화는 마지막 30분에 집중하는 전략을 세웠습니다.

---

## 2. 과제 진행에 있어 AI를 어떻게 활용하셨나요? 어떤 어려움이 있었나요?

Gen AI 플랫폼 개발자로서 저 역시 AI를 효과적으로 활용하는 능력이 중요하다고 판단하여 Claude Code CLI를 적극적으로 활용했습니다.

### 2.1 AI 활용 전략

AI를 단순한 코드 생성 도구가 아닌 **생산성 향상과 품질 보증을 위한 협력자**로 활용했습니다.

- **보일러플레이트 코드 생성 (약 30% 시간 절약)**:
    - **프롬프트 예시**: "Spring Boot + Kotlin으로 JWT 인증 필터 구현해줘. SecurityConfig와 연동되고Bearer 토큰 추출 로직 포함"
    - **활용**: `JwtAuthenticationFilter.kt`의 기본 구조를 빠르게 생성해 저는 비즈니스 로직 검토 및 예외 처리와 같은 핵심 작업에 집중할 수 있었습니다.
- **테스트 코드 작성 (품질 보증)**:
    - **프롬프트 예시**: "FeedbackService에 대한 단위 테스트 작성. MockK 사용, 중복 피드백 생성 시 예외 처리 테스트 포함"
    - **활용**: `FeedbackServiceTest.kt`의 초안을 생성하여, 저는 Edge case 시나리오 추가 및 테스트 커버리지 확장에 집중했습니다.
- **복잡한 쿼리 최적화**:
    - **문제**: "스레드별 채팅 그룹화 조회 시 N+1 문제 발생 우려"
    - **프롬프트 예시**: "JPA에서 Thread와 Chat의 관계를 Fetch Join으로 최적화하는 방법 알려줘"
    - **활용**: AI의 제안(예: `@EntityGraph` 또는 명시적 `join fetch` 쿼리)을 바탕으로 `ThreadRepository`에 최적화된 쿼리 메서드를 작성했습니다.
- **문서 자동 생성**: `README.md` 구조 제안, HTTP Client 테스트 파일 템플릿(`api-test.http`), Database 쿼리 모음 SQL 파일(`database-queries.sql`) 등 문서화 작업의 초안을 AI의 도움을 받아 빠르게 구성했습니다.

### 2.2 AI 활용의 어려움과 극복

AI는 강력한 도구이지만 맹신해서는 안 된다는 것을 여러 번 경험했습니다.

- **AI 생성 코드의 검증 필요성**:
    - **어려움**: AI가 제안한 Gemini API 호출 코드가 실제 API 스펙과 미묘하게 다르거나 모델 이름(`gemini-1.5-flash` vs `gemini-2.5-flash`)이 불일치하는 경우가 있었습니다. 특히 `gemini-2.5-flash` 모델은 `application.properties`에 설정되어 있었지만 `OpenAiClientImpl.kt`의 `@Value` 기본값은 `gemini-1.5-flash`로 되어 있어 혼란을 주었습니다.
    - **극복**: Gemini API 공식 문서를 직접 확인하고, `api-test.http` 파일을 통해 실제 API 호출을 반복적으로 수행하며 검증했습니다. AI의 제안을 참고하되 **비판적 사고**로 실제 동작을 검증하는 습관이 중요함을 다시 한번 깨달았습니다.
- **맥락(Context) 손실**:
    - **어려움**: AI가 이전 대화를 잊어버려 일관성 없는 코드 제안을 하는 경우가 있었습니다.
    - **극복**: 각 요청에 충분한 컨텍스트를 제공하고 "앞서 작성한 UserService와 일관되게"와 같은 명시적 지시를 통해 AI의 응답 품질을 높였습니다. 중요한 설계 결정은 문서화하여 AI에게 참조시켰습니다.
- **과도한 AI 의존 방지**:
    - **인식**: AI가 모든 것을 해결해줄 수는 없으며 핵심적인 비즈니스 로직과 설계 판단은 개발자의 몫이라는 것을 명확히 인식했습니다.
    - **대응**: 30분 스레드 로직과 같은 **핵심 비즈니스 로직**은 직접 설계하고 AI는 구현의 도구로만 활용했습니다. 최종 코드 리뷰와 테스트는 직접 수행하여 코드의 품질과 의도를 확인했습니다.

### 2.3 AI 활용으로 얻은 인사이트

AI는 개발자의 생산성을 극대화하는 강력한 도구이지만 그 결과물을 맹목적으로 신뢰하기보다는 **개발자의 비판적 사고와 검증 능력**이 필수적임을 배웠습니다. 실무에서는 AI를 활용하여 반복적인 작업을 줄이고 복잡한 문제 해결에 대한 아이디어를 얻으며 최종적으로는 더 높은 품질의 소프트웨어를 더 빠르게 개발하는 데 집중할 것입니다.

---

## 3. 구현하기 가장 어려웠던 기능 설명

### 어려웠던 기능: 30분 스레드 유지 로직과 AI 컨텍스트 관리

이 과제에서 가장 어려웠던 기능은 **30분 스레드 유지 로직**과 이에 따른 AI 컨텍스트 관리였습니다. AI 모델은 기본적으로 상태를 저장하지 않는(Stateless) 특성을 가지므로 이전 대화를 기억해야 하는 스레드 유지 로직을 어떻게 구현할지가 핵심 도전 과제였습니다.

### 문제 상황 분석 및 초기 고민

요구사항을 처음 읽었을 때 **30분 동안 같은 스레드를 유지한다는 것은 AI가 이전 대화를 기억해야 한다는 뜻이다. 그런데 AI 모델 자체는 상태를 저장하지 않는다(Stateless). 어떻게 구현할 것인가?**라는 질문이 가장 먼저 떠올랐습니다. 이는 단순히 DB에 `expiredAt` 같은 만료 시간을 저장하는 방식으로는 해결하기 어렵다고 판단했습니다. 왜냐하면 매번 새로운 채팅이 발생할 때마다 `expiredAt`을 갱신해야 하는 복잡성이 생기고 이는 버그 발생 가능성을 높일 수 있기 때문입니다.

### 기술적 도전 과제 및 해결책

1. **스레드 생성 시점 정확히 판단하기**:
    - **도전**: 사용자가 마지막 질문을 한 시점으로부터 30분이 경과했는지 정확히 판단하여 새 스레드를 생성하거나 기존 스레드를 유지해야 했습니다.
    - **해결책**: `Thread` 엔티티에 `lastActivityAt` (마지막 활동 시간) 컬럼을 추가하고 새로운 채팅이 발생할 때마다 이 시간을 `LocalDateTime.now()`로 갱신하도록 했습니다. 스레드의 만료 여부는 `isExpired()` 메서드에서 `LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(30))`와 같이 현재 시간과 `lastActivityAt`을 비교하여 계산하도록 구현했습니다. 이는 **만료 시간을 저장**하는 대신 **마지막 활동 시간을 저장하고 매번 만료 여부를 계산**하는 **'상태 계산'** 방식의 핵심 인사이트를 통해 해결했습니다.
    - **코드 예시 (Thread.kt)**:
        ```kotlin
        @Entity
        class Thread(
            @Column(nullable = false)
            var lastActivityAt: LocalDateTime = LocalDateTime.now() // 매 채팅마다 갱신
        ) {
            fun addChat(chat: Chat) {
                chats.add(chat)
                lastActivityAt = LocalDateTime.now() // 핵심: 매 채팅마다 갱신
            }
        
            fun isExpired(): Boolean {
                return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(30)) // 현재 시간과 비교하여 계산
            }
        }
        
        ```
        
    - **코드 예시 (ChatService.kt - getOrCreateActiveThread)**:
        ```kotlin
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
2. **AI에게 이전 대화 전달하기**:
    - **도전**: Gemini API는 Stateless이므로 이전 대화 내용을 매번 API 요청에 포함시켜야 AI가 컨텍스트를 유지하고 자연스러운 대화를 이어갈 수 있습니다. 하지만 너무 많은 대화를 보내면 토큰 비용이 증가할 수 있다는 점도 고려해야 했습니다.
    - **해결책**: 데이터베이스 스키마를 `Thread`와 `Chat` 간의 관계를 명확히 설정하여 특정 스레드에 속한 모든 대화(`Chat` 엔티티)를 `created_at` 기준으로 시간순으로 조회할 수 있도록 했습니다. 이후 `OpenAiClientImpl`의 `buildRequestBody` 메서드에서 이 `chatHistory`를 Gemini API의 `contents` 형식에 맞춰 변환하여 요청 본문에 포함시켰습니다.
    - **코드 예시 (buildRequestBody)**:
        ```kotlin
        private fun buildRequestBody(question: String, chatHistory: List<Chat>, context: String?): Map<String, Any> {
            val contents = mutableListOf<Map<String, Any>>()
            // ... RAG 컨텍스트 추가 로직 (생략)
        
            // 이전 대화 히스토리 추가 (시간순)
            chatHistory.forEach { chat ->
                contents.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to chat.question))))
                contents.add(mapOf("role" to "model", "parts" to listOf(mapOf("text" to chat.answer))))
            }
            // 현재 질문 추가 (생략)
            return mapOf("contents" to contents)
        }
        
        ```
    - **실제 API 호출 예시 (3번째 질문 시 Gemini API에 전송되는 데이터)**:


        ```json
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

### 검증 및 테스트

- **테스트 시나리오**: `api-test.http` 파일을 활용하여 연속 질문 시 동일 스레드가 유지되는지 30분 경과 후 질문 시 새 스레드가 생성되는지 시나리오를 직접 테스트하여 로직의 정확성을 검증했습니다.
- **실제 DB 검증**: 데이터베이스에서 `threads` 테이블의 `created_at`과 `last_activity_at` 컬럼을 확인하여 모든 연속 질문이 같은 스레드에 저장되고 만료 시 새 스레드가 생성되는 것을 확인했습니다.

### 이 경험을 통해 배운 것

- **복잡한 문제 분할**: **스레드 만료 판단**과 **컨텍스트 전달**이라는 두 개의 독립적인 문제로 분할하여 각각 해결한 후 통합하는 방식의 중요성을 깨달았습니다.
- **데이터베이스 설계의 중요성**: `lastActivityAt` 컬럼 하나가 전체 로직의 핵심이 되었으며 처음부터 완벽한 설계보다는 반복적인 개선의 중요성을 배웠습니다.
- **실무 관점의 트레이드오프**: 모든 대화를 AI에 전달하면 토큰 비용이 증가하고 최근 N개만 전달하면 컨텍스트 손실이 발생할 수 있다는 점을 인지하며 현재는 전체 전달 방식을 채택하고 향후 필요시 슬라이딩 윈도우 적용 가능성을 고려했습니다.

---

## 4. HTTP Client 검증

프로젝트의 모든 API 엔드포인트는 `api-test.http` 파일을 통해 직접 테스트하고 검증할 수 있습니다.


### 1. 회원가입
<img width="916" height="733" alt="image" src="https://github.com/user-attachments/assets/a9970484-75b8-46ba-8820-43c2b8cb65da" />

### 2. 로그인
<img width="907" height="701" alt="image" src="https://github.com/user-attachments/assets/d8dba763-61c0-40ff-9b6e-703471ed19e3" />

### 3. 채팅 생성
<img width="973" height="736" alt="image" src="https://github.com/user-attachments/assets/e15fc985-1412-4f00-ae49-8e540d7eded6" />

### 4. 스트리밍(SSE) 응답
<img width="2353" height="762" alt="image" src="https://github.com/user-attachments/assets/d7bd4318-3c2f-45d6-b3b9-c0cd356be9b9" />

### 5. 채팅 목록 조회
<img width="897" height="1260" alt="image" src="https://github.com/user-attachments/assets/f629d7f0-fa03-49e6-813c-6310480e6468" />

### 6. 스레드 삭제
<img width="867" height="532" alt="image" src="https://github.com/user-attachments/assets/b079608b-4c98-4e7d-babb-b545f671c374" />
<img width="862" height="532" alt="image" src="https://github.com/user-attachments/assets/d2301a6a-506f-48c1-a91e-d0a3f5a3f61f" />

### 7. 관리자 - 통계 조회
<img width="886" height="765" alt="image" src="https://github.com/user-attachments/assets/943fec9f-89c4-486f-b475-8affa834ee7b" />

### 8. 관리자 - 채팅 보고서 csv 다운로드
<img width="1166" height="820" alt="image" src="https://github.com/user-attachments/assets/38d5ad21-45e5-46b9-978b-56b37ac49249" />

### 9. 피드백 생성
### 긍정
<img width="949" height="859" alt="image" src="https://github.com/user-attachments/assets/7ea489a5-c79b-4908-a3c2-3c64f551e04e" />

### 부정
<img width="918" height="767" alt="image" src="https://github.com/user-attachments/assets/787c5995-9e45-42a5-b346-633573e3c063" />

### 10. 피드백 목록 조회
<img width="796" height="1261" alt="image" src="https://github.com/user-attachments/assets/71e0bef0-2757-4bf6-acf3-53e19cf404f9" />

### 긍정 필터
<img width="973" height="974" alt="image" src="https://github.com/user-attachments/assets/d70747c6-6f06-4767-a5cd-c88fb6221a9c" />

### 부정 필터
<img width="986" height="964" alt="image" src="https://github.com/user-attachments/assets/4c8e5ab9-d9d1-4d9e-90b8-a8f767aee919" />

### 11. 관리자 - 피드백 상태 변경
<img width="924" height="859" alt="image" src="https://github.com/user-attachments/assets/84d3083b-f55d-4353-9ca3-538d7e3f76c7" />

---

**작성자**: 석재민
<br>**작성일**: 2026-01-19
<br>**프로젝트 저장소**: https://github.com/0525aa36/intelligent-chatbot
<br>**연락처**: 0525aa36@gmail.com

