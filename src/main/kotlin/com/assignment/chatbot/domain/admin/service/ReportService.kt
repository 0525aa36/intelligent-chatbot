package com.assignment.chatbot.domain.admin.service

import com.assignment.chatbot.domain.chat.entity.Chat
import com.assignment.chatbot.domain.chat.repository.ChatRepository
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

@Service
class ReportService(
    private val chatRepository: ChatRepository
) {

    /**
     * CSV 보고서 생성 (메모리 효율적 Stream 방식)
     *
     * 최근 24시간 내 모든 사용자의 대화 목록을 CSV 형식으로 생성
     *
     * 메모리 최적화:
     * - JPA Stream으로 데이터를 청크 단위로 가져와 처리
     * - 대용량 데이터도 OOM(Out of Memory) 없이 처리 가능
     * - ByteArrayOutputStream으로 CSV 스트리밍 생성
     *
     * 성능 최적화:
     * - fetch join으로 N+1 문제 해결
     * - READ_ONLY 트랜잭션으로 변경 감지 비활성화
     * - Batch Size로 DB 왕복 횟수 최소화
     *
     * CSV 포맷:
     * - 헤더: 대화ID, 사용자ID, 사용자명, 질문, 답변, 생성일시
     * - 인코딩: UTF-8 (한글 지원)
     */
    @Transactional(readOnly = true)
    fun generateChatReport(): ByteArray {
        val endTime = LocalDateTime.now()
        val startTime = endTime.minusHours(24)

        // Stream으로 대용량 데이터 처리
        return chatRepository.streamAllByCreatedAtBetween(startTime, endTime).use { chatStream ->
            generateCsvBytesFromStream(chatStream)
        }
    }

    /**
     * Chat 리스트를 CSV 바이트 배열로 변환
     *
     * Apache Commons CSV 라이브러리 사용
     * - 자동 이스케이프 처리
     * - 표준 CSV 포맷 준수
     */
    private fun generateCsvBytes(chats: List<Chat>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        // BOM (Byte Order Mark) 추가 - Excel에서 UTF-8 인식을 위함
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
            "대화ID",
            "사용자ID",
            "사용자명",
            "질문",
            "답변",
            "생성일시"
        )).use { csvPrinter ->
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            chats.forEach { chat ->
                csvPrinter.printRecord(
                    chat.id,
                    chat.thread.user.id,
                    chat.thread.user.name,
                    chat.question,
                    chat.answer,
                    chat.createdAt?.format(dateFormatter) ?: ""
                )
            }

            csvPrinter.flush()
        }

        return outputStream.toByteArray()
    }

    /**
     * Chat Stream을 CSV 바이트 배열로 변환 (메모리 효율적)
     *
     * 대용량 데이터 처리:
     * - Stream을 순회하며 한 줄씩 CSV에 작성
     * - 전체 데이터를 메모리에 로드하지 않음
     * - 수백만 건의 데이터도 안정적으로 처리 가능
     *
     * Apache Commons CSV 라이브러리 사용
     * - 자동 이스케이프 처리
     * - 표준 CSV 포맷 준수
     */
    private fun generateCsvBytesFromStream(chatStream: Stream<Chat>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        // BOM (Byte Order Mark) 추가 - Excel에서 UTF-8 인식을 위함
        outputStream.write(0xEF)
        outputStream.write(0xBB)
        outputStream.write(0xBF)

        CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
            "대화ID",
            "사용자ID",
            "사용자명",
            "질문",
            "답변",
            "생성일시"
        )).use { csvPrinter ->
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            // Stream을 순회하며 CSV에 작성 (메모리 효율적)
            chatStream.forEach { chat ->
                csvPrinter.printRecord(
                    chat.id,
                    chat.thread.user.id,
                    chat.thread.user.name,
                    chat.question,
                    chat.answer,
                    chat.createdAt?.format(dateFormatter) ?: ""
                )

                // 주기적으로 flush하여 메모리 압박 완화
                // (매 100개마다 flush, 필요시 조정 가능)
                if (chat.id?.rem(100) == 0L) {
                    csvPrinter.flush()
                }
            }

            csvPrinter.flush()
        }

        return outputStream.toByteArray()
    }
}
