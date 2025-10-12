package com.smwu.matchalot.application.service;

import com.smwu.matchalot.domain.model.entity.Notification.NotificationType;
import com.smwu.matchalot.domain.model.vo.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.reactive.ReactiveMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ReactiveMailSender mailSender;
    
    @Value("${spring.mail.username:noreply@match-a-lot.store}")
    private String fromEmail;
    
    @Value("${app.frontend.url:https://www.match-a-lot.store}")
    private String frontendUrl;
    
    public Mono<Void> sendNotificationEmail(Email to, NotificationType type, String title, String message) {
        return Mono.fromCallable(() -> {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to.value());
            mailMessage.setSubject("[Match-A-Lot] " + title);
            mailMessage.setText(buildEmailBody(type, title, message));
            return mailMessage;
        })
        .flatMap(mailSender::send)
        .doOnSuccess(v -> log.info("이메일 전송 성공: {} -> {}", type, to.value()))
        .doOnError(e -> log.error("이메일 전송 실패: {} -> {}, 오류: {}", type, to.value(), e.getMessage()))
        .onErrorResume(e -> {
            // 이메일 전송 실패해도 서비스는 계속 동작
            log.warn("이메일 전송 실패 무시: {}", e.getMessage());
            return Mono.empty();
        })
        .subscribeOn(Schedulers.boundedElastic()) // 블로킹 I/O를 위한 스케줄러
        .then();
    }
    
    private String buildEmailBody(NotificationType type, String title, String message) {
        StringBuilder body = new StringBuilder();
        body.append("안녕하세요, Match-A-Lot입니다.\n\n");
        
        // 알림 타입별 맞춤 메시지
        switch (type) {
            case USER_PROMOTED:
                body.append("🎉 축하합니다!\n");
                body.append(message).append("\n\n");
                body.append("이제 더 많은 기능을 이용하실 수 있습니다.\n");
                break;
                
            case MATERIAL_APPROVED:
                body.append("✅ 족보 승인 완료\n");
                body.append(message).append("\n\n");
                body.append("다른 사용자들과 매칭을 시작해보세요!\n");
                break;
                
            case MATERIAL_REJECTED:
                body.append("❌ 족보 승인 거절\n");
                body.append(message).append("\n\n");
                body.append("자세한 사항은 사이트에서 확인해주세요.\n");
                break;
                
            case MATCH_COMPLETED:
                body.append("🤝 매칭 성공!\n");
                body.append(message).append("\n\n");
                body.append("지금 바로 족보를 확인해보세요.\n");
                break;
                
            case MATCH_REQUEST_RECEIVED:
                body.append("📬 새로운 매칭 요청\n");
                body.append(message).append("\n\n");
                body.append("매칭 요청을 확인하고 응답해주세요.\n");
                break;
                
            default:
                body.append(message).append("\n\n");
        }
        
        body.append("\n");
        body.append("자세한 내용은 아래 링크에서 확인하세요:\n");
        body.append(frontendUrl).append("\n\n");
        body.append("감사합니다.\n");
        body.append("Match-A-Lot 팀 드림\n");
        body.append("\n---\n");
        body.append("이 메일은 발신 전용입니다. 문의사항은 사이트 내 문의하기를 이용해주세요.\n");
        
        return body.toString();
    }
    
    public Mono<Void> sendWelcomeEmail(Email to, String nickname) {
        return Mono.fromCallable(() -> {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to.value());
            mailMessage.setSubject("[Match-A-Lot] 환영합니다, " + nickname + "님!");
            mailMessage.setText(buildWelcomeEmailBody(nickname));
            return mailMessage;
        })
        .flatMap(mailSender::send)
        .doOnSuccess(v -> log.info("환영 이메일 전송 성공: {}", to.value()))
        .doOnError(e -> log.error("환영 이메일 전송 실패: {}, 오류: {}", to.value(), e.getMessage()))
        .onErrorResume(e -> Mono.empty())
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    private String buildWelcomeEmailBody(String nickname) {
        return String.format("""
            안녕하세요, %s님!
            
            Match-A-Lot에 가입하신 것을 환영합니다! 🎉
            
            Match-A-Lot은 족보 교환 플랫폼으로, 서로 필요한 자료를 공유하며 함께 성장하는 공간입니다.
            
            시작하기:
            1. 첫 족보를 업로드해보세요
            2. 승인이 완료되면 정회원으로 승격됩니다
            3. 다른 사용자와 매칭을 시작하세요!
            
            사이트 바로가기: %s
            
            궁금한 점이 있으시면 언제든 문의해주세요.
            
            감사합니다.
            Match-A-Lot 팀 드림
            """, nickname, frontendUrl);
    }
}