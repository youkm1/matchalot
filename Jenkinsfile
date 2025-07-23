import groovy.json.JsonBuilder

pipeline {
    agent any
    
    triggers {
        // ✅ Generic Webhook Trigger로 변경
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref'],
                [key: 'repository_name', value: '$.repository.name'],
                [key: 'pusher_name', value: '$.pusher.name'],
                [key: 'commits', value: '$.commits']
            ],
            token: 'matchalot-backend',
            printContributedVariables: true,
            printPostContent: true,
            silentResponse: false,
            // ✅ main 브랜치에서만 트리거 (선택사항)
            regexpFilterText: '$ref',
            regexpFilterExpression: 'refs/heads/(main|develop)'
        )
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
        stage('Webhook Debug') {
            steps {
                script {
                    echo "🔍 Webhook Variables:"
                    echo "ref: ${env.ref ?: 'Not set'}"
                    echo "repository_name: ${env.repository_name ?: 'Not set'}"
                    echo "pusher_name: ${env.pusher_name ?: 'Not set'}"
                    echo "Branch Name: ${env.BRANCH_NAME ?: 'Not detected'}"
                    
                    // 브랜치 이름 추출
                    if (env.ref) {
                        env.EXTRACTED_BRANCH = env.ref.replaceAll(/refs\/heads\//, '')
                        echo "추출된 브랜치: ${env.EXTRACTED_BRANCH}"
                    }
                }
            }
        }
        
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
                    ./gradlew clean test
                '''
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
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
                        echo "보안 스캔 완료 (경고 있음): ${e.getMessage()}"
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
                    expression { env.EXTRACTED_BRANCH == 'main' }
                    expression { env.EXTRACTED_BRANCH == 'develop' }
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
                    expression { env.EXTRACTED_BRANCH == 'main' }
                    expression { env.EXTRACTED_BRANCH == 'develop' }
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '백엔드 배포'
                script {
                    def currentBranch = env.EXTRACTED_BRANCH ?: env.BRANCH_NAME
                    def deployEnv = currentBranch == 'main' ? 'production' : 'staging'
                    
                    sh """
                        ssh -o StrictHostKeyChecking=no ${deployEnv}-server '
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
                    expression { env.EXTRACTED_BRANCH == 'main' }
                    expression { env.EXTRACTED_BRANCH == 'develop' }
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                echo '백엔드 헬스체크'
                script {
                    sleep(time: 30, unit: 'SECONDS')  // 서비스 시작 대기
                    
                    def maxRetries = 6
                    def retryCount = 0
                    def healthOk = false
                    
                    while (retryCount < maxRetries && !healthOk) {
                        try {
                            sh '''
                                curl --silent --show-error -f https://matchalot.duckdns.org/api/actuator/health
                                curl --silent --show-error -f https://matchalot.duckdns.org/api/v1/study-materials/subjects
                            '''
                            healthOk = true
                            echo "백엔드 헬스체크 성공! (시도: ${retryCount + 1})"
                        } catch (Exception e) {
                            retryCount++
                            echo "백엔드 헬스체크 실패, 재시도 중... (${retryCount}/${maxRetries}): ${e.getMessage()}"
                            sleep(time: 15, unit: 'SECONDS')
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
                try {
                    def currentBranch = env.EXTRACTED_BRANCH ?: env.BRANCH_NAME
                    def deployEnv = currentBranch == 'main' ? '프로덕션' : '스테이징'
                    def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                    def commitAuthor = env.pusher_name ?: sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                    def branchName = currentBranch ?: 'unknown'
                    def buildNumber = env.BUILD_NUMBER
                    
                    def jsonPayload = """{
                    "embeds": [{
                        "title": "🚀 Backend 배포 성공!",
                        "description": "**${deployEnv}** 환경에 백엔드가 배포되었습니다.",
                        "color": 3066993,
                        "fields": [
                            {
                                "name": "브랜치",
                                "value": "`${branchName}`",
                                "inline": true
                            },
                            {
                                "name": "빌드 번호",
                                "value": "`#${buildNumber}`",
                                "inline": true
                            },
                            {
                                "name": "커밋 작성자",
                                "value": "${commitAuthor}",
                                "inline": true
                            },
                            {
                                "name": "API 확인",
                                "value": "[Backend API](https://matchalot.duckdns.org/)",
                                "inline": false
                            }
                        ],
                        "timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"))}"
                    }]
                }"""
                    
                    sh """
                        curl -H "Content-Type: application/json" \\
                             -X POST \\
                             -d '${jsonPayload}' \\
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
                try {
                    def currentBranch = env.EXTRACTED_BRANCH ?: env.BRANCH_NAME
                    def deployEnv = currentBranch == 'main' ? '프로덕션' : '스테이징'
                    def commitAuthor = env.pusher_name ?: sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                    def branchName = currentBranch ?: 'unknown'
                    def buildNumber = env.BUILD_NUMBER ?: 'unknown'
                    def buildUrl = env.BUILD_URL ?: 'unknown'
                    
                    def jsonPayload = """{
                    "embeds": [{
                        "title": "💥 Backend 배포 실패!",
                        "description": "**${deployEnv}** 환경 백엔드 배포 중 오류가 발생했습니다.",
                        "color": 15158332,
                        "fields": [
                            {
                                "name": "브랜치",
                                "value": "`${branchName}`",
                                "inline": true
                            },
                            {
                                "name": "빌드 번호",
                                "value": "`#${buildNumber}`",
                                "inline": true
                            },
                            {
                                "name": "커밋 작성자",
                                "value": "${commitAuthor}",
                                "inline": true
                            },
                            {
                                "name": "로그 확인",
                                "value": "[Jenkins 콘솔](${buildUrl}console)",
                                "inline": false
                            }
                        ],
                        "timestamp": "${new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"))}"
                    }]
                }"""
                    
                    sh """
                        curl -H "Content-Type: application/json" \\
                             -X POST \\
                             -d '${jsonPayload}' \\
                             ${DISCORD_WEBHOOK}
                    """
                } catch (Exception e) {
                    echo "Discord 알림 실패: ${e.getMessage()}"
                }
            }
        }
        
        always {
            script {
                try {
                    // 아티팩트 아카이브
                    archiveArtifacts artifacts: 'build/reports/**/*', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true
                } catch (Exception e) {
                    echo "아티팩트 아카이브 실패: ${e.getMessage()}"
                }
                
                try {
                    // Docker 정리
                    sh '''
                        docker image prune -f --filter "dangling=true" || true
                        docker images ${IMAGE_NAME} --format "table {{.Repository}}:{{.Tag}}" | \\
                        grep -E "${IMAGE_NAME}:[0-9]+" | \\
                        sort -r | \\
                        tail -n +6 | \\
                        xargs --no-run-if-empty docker rmi || true
                    '''
                } catch (Exception e) {
                    echo "Docker 정리 실패: ${e.getMessage()}"
                }
            }
        }
    }
}
