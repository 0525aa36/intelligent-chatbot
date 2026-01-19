package com.assignment.chatbot.global.security

import com.assignment.chatbot.domain.user.entity.User
import com.assignment.chatbot.domain.user.entity.UserRole
import com.assignment.chatbot.global.exception.CustomException
import com.assignment.chatbot.global.exception.ErrorCode
import org.springframework.stereotype.Component

/**
 * 권한 검증 유틸리티
 *
 * 책임:
 * - 사용자별 리소스 접근 권한 검증
 * - 관리자 권한 확인
 * - 소유자 권한 확인
 *
 * 보안 원칙:
 * - Fail-Safe: 권한이 명확하지 않으면 거부
 * - Principle of Least Privilege: 최소 권한 원칙
 * - Defense in Depth: 다층 방어
 *
 * 사용 예시:
 * ```kotlin
 * authorizationChecker.checkOwnerOrAdmin(
 *     currentUser = currentUser,
 *     resourceOwnerId = chat.thread.user.id,
 *     resourceName = "대화"
 * )
 * ```
 */
@Component
class AuthorizationChecker {

    /**
     * 리소스 소유자 또는 관리자 권한 확인
     *
     * @param currentUser 현재 사용자
     * @param resourceOwnerId 리소스 소유자 ID
     * @param resourceName 리소스 이름 (에러 메시지용)
     * @throws CustomException 권한이 없는 경우 FORBIDDEN
     */
    fun checkOwnerOrAdmin(
        currentUser: User,
        resourceOwnerId: Long?,
        resourceName: String = "리소스"
    ) {
        // 관리자는 모든 리소스 접근 가능
        if (currentUser.role == UserRole.ADMIN) {
            return
        }

        // 소유자 확인
        if (currentUser.id != resourceOwnerId) {
            throw CustomException(
                ErrorCode.FORBIDDEN,
                "다른 사용자의 ${resourceName}에 접근할 수 없습니다."
            )
        }
    }

    /**
     * 관리자 권한 확인
     *
     * @param currentUser 현재 사용자
     * @throws CustomException 관리자가 아닌 경우 FORBIDDEN
     */
    fun checkAdmin(currentUser: User) {
        if (currentUser.role != UserRole.ADMIN) {
            throw CustomException(
                ErrorCode.FORBIDDEN,
                "관리자 권한이 필요합니다."
            )
        }
    }

    /**
     * 리소스 소유자 권한 확인 (관리자 제외)
     *
     * @param currentUser 현재 사용자
     * @param resourceOwnerId 리소스 소유자 ID
     * @param resourceName 리소스 이름 (에러 메시지용)
     * @throws CustomException 소유자가 아닌 경우 FORBIDDEN
     */
    fun checkOwnerOnly(
        currentUser: User,
        resourceOwnerId: Long?,
        resourceName: String = "리소스"
    ) {
        if (currentUser.id != resourceOwnerId) {
            throw CustomException(
                ErrorCode.FORBIDDEN,
                "본인의 ${resourceName}만 접근할 수 있습니다."
            )
        }
    }

    /**
     * 여러 사용자 중 하나인지 확인
     *
     * @param currentUser 현재 사용자
     * @param allowedUserIds 허용된 사용자 ID 목록
     * @throws CustomException 허용된 사용자가 아닌 경우 FORBIDDEN
     */
    fun checkUserInList(
        currentUser: User,
        allowedUserIds: List<Long>,
        resourceName: String = "리소스"
    ) {
        // 관리자는 항상 허용
        if (currentUser.role == UserRole.ADMIN) {
            return
        }

        if (currentUser.id !in allowedUserIds) {
            throw CustomException(
                ErrorCode.FORBIDDEN,
                "${resourceName}에 접근 권한이 없습니다."
            )
        }
    }

    /**
     * 사용자가 관리자인지 확인 (boolean 반환)
     *
     * @param user 확인할 사용자
     * @return 관리자 여부
     */
    fun isAdmin(user: User): Boolean {
        return user.role == UserRole.ADMIN
    }

    /**
     * 사용자가 리소스 소유자인지 확인 (boolean 반환)
     *
     * @param user 확인할 사용자
     * @param resourceOwnerId 리소스 소유자 ID
     * @return 소유자 여부
     */
    fun isOwner(user: User, resourceOwnerId: Long?): Boolean {
        return user.id == resourceOwnerId
    }
}
