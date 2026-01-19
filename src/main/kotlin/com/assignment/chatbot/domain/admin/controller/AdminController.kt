package com.assignment.chatbot.domain.admin.controller

import com.assignment.chatbot.domain.admin.dto.StatisticsResponse
import com.assignment.chatbot.domain.admin.service.ReportService
import com.assignment.chatbot.domain.admin.service.StatisticsService
import com.assignment.chatbot.global.common.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val statisticsService: StatisticsService,
    private val reportService: ReportService
) {

    /**
     * 사용자 활동 통계 조회 API
     * 
     * 권한: 관리자만 접근 가능 (ADMIN)
     * 기능: 최근 24시간 내 회원가입, 로그인, 대화 생성 수 집계
     * 
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": {
     *     "period": "최근 24시간",
     *     "startTime": "2025-01-15T10:00:00",
     *     "endTime": "2025-01-16T10:00:00",
     *     "signUpCount": 15,
     *     "loginCount": 15,
     *     "chatCount": 42
     *   }
     * }
     */
    @GetMapping("/statistics")
    fun getStatistics(): ResponseEntity<ApiResponse<StatisticsResponse>> {
        val statistics = statisticsService.getUserActivityStatistics()
        return ResponseEntity.ok(ApiResponse.success(statistics))
    }

    /**
     * CSV 보고서 생성 API
     * 
     * 권한: 관리자만 접근 가능 (ADMIN)
     * 기능: 최근 24시간 내 모든 사용자의 대화 목록을 CSV 파일로 다운로드
     * 
     * 파일명: chat_report_YYYYMMDD_HHmmss.csv
     * 인코딩: UTF-8 with BOM (Excel 호환)
     * 
     * CSV 포맷:
     * 대화ID, 사용자ID, 사용자명, 질문, 답변, 생성일시
     */
    @GetMapping("/reports/chat")
    fun downloadChatReport(): ResponseEntity<ByteArray> {
        val csvBytes = reportService.generateChatReport()
        
        // 파일명 생성 (타임스탬프 포함)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "chat_report_$timestamp.csv"
        
        // HTTP 헤더 설정
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("text/csv; charset=UTF-8")
            setContentDispositionFormData("attachment", filename)
            contentLength = csvBytes.size.toLong()
        }
        
        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(headers)
            .body(csvBytes)
    }
}
