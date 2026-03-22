package com.fabbitinc.server.application.chat.support;

import org.springframework.stereotype.Component;

@Component
public class ChatMessageCatalog {

    public String defaultThreadTitle() {
        return "새 챗";
    }

    public String runReasoningInProgress() {
        return "질문을 이해하고 필요한 도구를 판단하고 있습니다";
    }

    public String chatAgentFailed() {
        return "챗 응답 생성 중 오류가 발생했습니다.";
    }

    public String pendingIssueDraftReady() {
        return "이슈 초안을 만들었습니다. 아래 카드에서 생성 또는 취소를 선택해 주세요.";
    }

    public String resultsPrepared() {
        return "요청에 필요한 조회 결과를 정리했습니다.";
    }

    public String requestProcessed() {
        return "요청을 처리했습니다.";
    }

    public String sanitizedFallback() {
        return "요청 결과를 정리했습니다.";
    }

    public String partLookupStarted() {
        return "부품을 찾는 중입니다";
    }

    public String partLookupSummary(int count) {
        return "후보 " + count + "건을 찾았습니다";
    }

    public String partLookupProgress(int count) {
        return "부품 후보 " + count + "건을 찾았습니다.";
    }

    public String partLookupTraceCompleted() {
        return "품번 후보를 조회했습니다";
    }

    public String partLookupResponseSummary(int count) {
        return "품번 후보 " + count + "건을 찾았습니다.";
    }

    public String partListTitle() {
        return "부품 후보";
    }

    public String partIssueLookupStarted() {
        return "관련 이슈를 찾는 중입니다";
    }

    public String partIssueLookupSummary(int count) {
        return "연결 이슈 " + count + "건을 찾았습니다";
    }

    public String partIssueLookupProgress(int count) {
        return "관련 이슈 " + count + "건을 찾았습니다.";
    }

    public String partIssueLookupNotFound() {
        return "대상 부품을 찾지 못했습니다.";
    }

    public String partIssueLookupTraceCompleted() {
        return "부품과 연결된 이슈를 조회했습니다";
    }

    public String partIssueLookupResponseSummary(int count) {
        return "연결 이슈 " + count + "건을 찾았습니다.";
    }

    public String partDetailTitle() {
        return "대상 부품";
    }

    public String issueListTitle() {
        return "연결 이슈";
    }

    public String issueDraftStarted() {
        return "이슈 초안을 만드는 중입니다";
    }

    public String issueDraftWaitingSummary() {
        return "확인 대기 중인 이슈 초안을 만들었습니다";
    }

    public String issueDraftCreated() {
        return "이슈 초안을 만들었습니다.";
    }

    public String issueDraftTraceCompleted() {
        return "이슈 초안을 만들었습니다";
    }

    public String issueDraftCreatedResponse() {
        return "이슈 초안을 만들었습니다. 사용자가 확인하면 실제 생성됩니다.";
    }

    public String issueDraftTargetPartNotFound() {
        return "대상 부품을 찾지 못했습니다.";
    }

    public String issueDraftTargetPartMissingResponse() {
        return "대상 부품을 찾지 못해서 이슈 초안을 만들 수 없습니다.";
    }

    public String issueDraftDefaultTitle() {
        return "챗 이슈 초안";
    }

    public String issueDraftDefaultBody() {
        return "챗에서 생성한 이슈입니다.";
    }

    public String unknownPartDisplayName() {
        return "알 수 없는 부품";
    }

    public String actionRequestPayloadReadFailed() {
        return "액션 요청 payload를 읽을 수 없습니다";
    }

    public String actionPayloadSerializationFailed() {
        return "액션 payload 직렬화에 실패했습니다";
    }

    public String unsupportedChatActionType() {
        return "지원하지 않는 챗 액션 타입입니다";
    }

    public String issueCreateConfirmedTrace() {
        return "이슈 생성 요청을 실행했습니다";
    }

    public String issueCreated() {
        return "이슈를 생성했습니다.";
    }

    public String actionCancelled() {
        return "초안 실행을 취소했습니다.";
    }
}
