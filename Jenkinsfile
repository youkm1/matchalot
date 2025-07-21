pipeline {
    agent any
    
    triggers {
        githubPush()  // Git Push 시 자동 트리거
    }
    
    environment {
        IMAGE_NAME = 'matchalot-backend'
        DOCKER_REGISTRY = 'your-registry'  // Docker Hub 또는 ECR
        BUILD_TIMESTAMP = sh(script: 'date +%Y%m%d_%H%M%S', returnStdout: true).trim()
        DISCORD_WEBHOOK = 'https://discord.com/api/webhooks/1394791850098950264/7TzxWVNuVkId9gcLg-E-j6aREbRwsQ79_jGKA-NUkYr1K_9sd9t9yGTYiyVSBGAxZcYm'
    }
    
    options {
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '백엔드 소스코드 체크아웃'
                checkout scm
            }
        }
        
        stage('Test') {
            steps {
                echo '백엔드 테스트 실행'
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean test jacocoTestReport
                '''
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'build/test-results/test/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/jacoco/test/html',
                        reportFiles: 'index.html',
                        reportName: 'Backend Coverage Report'
                    ])
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                echo '백엔드 보안 스캔'
                script {
                    try {
                        sh './gradlew dependencyCheckAnalyze || true'
                    } catch (Exception e) {
                        echo "보안 스캔 완료 (경고 있음)"
                    }
                }
            }
        }
        
        stage('Build') {
            steps {
                echo '백엔드 JAR 빌드'
                sh '''
                    ./gradlew clean build -x test
                '''
            }
        }
        
        stage('Build Docker Image') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '백엔드 Docker 이미지 빌드'
                script {
                    def image = docker.build("${IMAGE_NAME}:${BUILD_NUMBER}")
                    
                    // Docker Hub에 푸시
                    docker.withRegistry('', 'docker-hub-credentials') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }
        
        stage('Deploy') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '백엔드 배포'
                script {
                    def deployEnv = env.BRANCH_NAME == 'main' ? 'production' : 'staging'
                    
                    sh """
                        # DevOps 서버에 배포
                        ssh ${deployEnv}-server '
                            cd /opt/matchalot/devops &&
                            docker-compose pull backend &&
                            docker-compose up -d backend
                        '
                    """
                }
            }
        }
        
        stage('Health Check') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '백엔드 헬스체크'
                script {
                    sleep(30)  // 서비스 시작 대기
                    
                    def maxRetries = 6
                    def retryCount = 0
                    def healthOk = false
                    
                    while (retryCount < maxRetries && !healthOk) {
                        try {
                            sh '''
                                curl -f https://matchalot.duckdns.org/api/actuator/health
                                curl -f https://matchalot.duckdns.org/api/v1/study-materials/subjects
                            '''
                            healthOk = true
                            echo "백엔드 헬스체크 성공! (시도: ${retryCount + 1})"
                        } catch (Exception e) {
                            retryCount++
                            echo "백엔드 헬스체크 실패, 재시도 중... (${retryCount}/${maxRetries})"
                            sleep(15)
                        }
                    }
                    
                    if (!healthOk) {
                        error "백엔드 헬스체크 최종 실패!"
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '백엔드 배포 성공!'
            script {
                def deployEnv = env.BRANCH_NAME == 'main' ? '프로덕션' : '스테이징'
                def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                def commitAuthor = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                
                try {
                    sh """
                        curl -H "Content-Type: application/json" \\
                             -X POST \\
                             -d '{
                               "embeds": [{
                                 "title": "🚀 Backend 배포 성공!",
                                 "description": "**${deployEnv}** 환경에 백엔드가 배포되었습니다.",
                                 "color": 3066993,
                                 "fields": [
                                   {
                                     "name": "브랜치",
                                     "value": "\\`${env.BRANCH_NAME}\\`",
                                     "inline": true
                                   },
                                   {
                                     "name": "빌드 번호",
                                     "value": "\\`#${env.BUILD_NUMBER}\\`",
                                     "inline": true
                                   },
                                   {
                                     "name": "커밋 작성자",
                                     "value": "\\`${commitAuthor}\\`",
                                     "inline": true
                                   },
                                   {
                                     "name": "API 확인",
                                     "value": "[Backend API](https://matchalot.duckdns.org/)",
                                     "inline": false
                                   }
                                 ],
                                 "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'"
                               }]
                             }' \\
                             ${DISCORD_WEBHOOK}
                    """
                } catch (Exception e) {
                    echo "Discord 알림 실패: ${e.getMessage()}"
                }
            }
        }
        
        failure {
            echo '백엔드 배포 실패!'
            script {
                def commitAuthor = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                
                try {
                    sh """
                        curl -H "Content-Type: application/json" \\
                             -X POST \\
                             -d '{
                               "embeds": [{
                                 "title": "💥 Backend 배포 실패!",
                                 "description": "백엔드 배포 중 오류가 발생했습니다.",
                                 "color": 15158332,
                                 "fields": [
                                   {
                                     "name": "브랜치",
                                     "value": "\\`${env.BRANCH_NAME}\\`",
                                     "inline": true
                                   },
                                   {
                                     "name": "빌드 번호",
                                     "value": "\\`#${env.BUILD_NUMBER}\\`",
                                     "inline": true
                                   },
                                   {
                                     "name": "로그 확인",
                                     "value": "[Jenkins 콘솔](${env.BUILD_URL}console)",
                                     "inline": false
                                   }
                                 ],
                                 "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'"
                               }]
                             }' \\
                             ${DISCORD_WEBHOOK}
                    """
                } catch (Exception e) {
                    echo "Discord 알림 실패: ${e.getMessage()}"
                }
            }
        }
        
        always {
            archiveArtifacts artifacts: 'build/reports/**/*', allowEmptyArchive: true
            archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true
            
            // Docker 정리
            script {
                try {
                    sh '''
                        docker image prune -f --filter "dangling=true"
                        docker images ${IMAGE_NAME} --format "table {{.Repository}}:{{.Tag}}" | \\
                        grep -E "${IMAGE_NAME}:[0-9]+" | \\
                        tail -n +6 | \\
                        xargs -r docker rmi || true
                    '''
                } catch (Exception e) {
                    echo "Docker 정리 실패: ${e.getMessage()}"
                }
            }
        }
    }
}

