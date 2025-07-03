pipeline {
	agent any

    environment {
		// ======== CONFIGURABLE VARIABLES (MODIFICA AQUÍ PARA OTROS PROYECTOS) ========
        PROJECT_NAME = 'payment-service'
        GITHUB_REPO = 'payment-service-braidsbeautyByAngie-'
        GITHUB_OWNER = 'RPantaX'
        GITHUB_CREDENTIALS_ID = 'github-token'
        GITHUB_TOKEN_ID = 'github-token-2' // Para autenticación de GitHub Packages
        DOCKER_HUB_NAMESPACE = 'rpantax'
        DOCKER_CREDENTIALS_ID = 'jenkins-cicd-token2'
        DEFAULT_BRANCH = 'main'
        // ==============================================================================

        DOCKER_HUB_REPO = "${DOCKER_HUB_NAMESPACE}/${PROJECT_NAME}"
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        GITHUB_USERNAME = "${GITHUB_OWNER}"
        GITHUB_TOKEN = credentials("${GITHUB_TOKEN_ID}")
        CURRENT_BRANCH = "${env.BRANCH_NAME ?: DEFAULT_BRANCH}"
    }

    tools {
		maven 'maven4.0.0'
    }

    stages {
		stage('Verify GitHub Access') {
			steps {
				echo 'Verifying GitHub Packages access...'
                sh '''
                    echo "Testing GitHub authentication..."
                    HTTP_STATUS=$(curl -u ${GITHUB_USERNAME}:${GITHUB_TOKEN} \
                        -s -o /dev/null -w "%{http_code}" \
                        https://maven.pkg.github.com/${GITHUB_OWNER}/core-service-braidsbeautyByAngie/com/braidsbeautyByAngie/saga-pattern-spring-boot/maven-metadata.xml)

                    echo "HTTP Status Code: $HTTP_STATUS"
                    if [ "$HTTP_STATUS" = "200" ]; then
                        echo "✅ GitHub authentication successful"
                        curl -u ${GITHUB_USERNAME}:${GITHUB_TOKEN} \
                            https://maven.pkg.github.com/${GITHUB_OWNER}/core-service-braidsbeautyByAngie/com/braidsbeautyByAngie/saga-pattern-spring-boot/maven-metadata.xml \
                            -s | head -10
                    else
                        echo "❌ GitHub authentication failed with status: $HTTP_STATUS"
                        exit 1
                    fi
                '''
            }
        }

        stage('Clone Repo') {
			steps {
				echo "Checking out code from ${env.CURRENT_BRANCH} branch"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${DEFAULT_BRANCH}"]],
                    userRemoteConfigs: [[
                        url: "https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}.git",
                        credentialsId: "${GITHUB_CREDENTIALS_ID}"
                    ]]
                ])
                script {
					env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    env.CURRENT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                    env.DOCKER_IMAGE_TAG = "${BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                    echo "Building from branch: ${env.CURRENT_BRANCH}"
                    echo "Git commit: ${env.GIT_COMMIT}"
                    echo "Docker tag: ${env.DOCKER_IMAGE_TAG}"
                }
            }
        }

        stage('Clean & Compile') {
			steps {
				echo 'Cleaning and compiling the project...'
                withCredentials([string(credentialsId: "${GITHUB_TOKEN_ID}", variable: 'GITHUB_TOKEN')]) {
					sh '''
                        echo "=== Generating Maven settings.xml ==="
                        cat > settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

                        mvn -version
                        java -version
                        mvn clean package -DskipTests --settings settings.xml
                    '''
                }
            }
        }

        stage('Docker Build') {
			steps {
				echo 'Building Docker image...'
                script {
					def dockerImage = docker.build("${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG}")
                    env.DOCKER_IMAGE_ID = dockerImage.id
                    dockerImage.tag("${env.CURRENT_BRANCH}-latest")
                    if (env.CURRENT_BRANCH == 'main') {
						dockerImage.tag('latest')
                    }
                    echo "Docker image built: ${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG}"
                }
            }
        }

        stage('Docker Push') {
			when {
				anyOf {
					branch 'main'
                    branch 'develop'
                    expression { env.CURRENT_BRANCH == 'main' }
                    expression { env.CURRENT_BRANCH == 'develop' }
                }
            }
            steps {
				echo 'Pushing Docker image to Docker Hub...'
                script {
					docker.withRegistry('https://index.docker.io/v1/', "${DOCKER_CREDENTIALS_ID}") {
						def image = docker.image("${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG}")
                        image.push()
                        image.push("${env.CURRENT_BRANCH}-latest")
                        if (env.CURRENT_BRANCH == 'main') {
							image.push('latest')
                        }
                    }
                }
                echo "Image pushed: ${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG}"
            }
        }

        stage('Cleanup') {
			steps {
				echo 'Cleaning up local Docker images...'
                script {
					sh """
                        docker rmi ${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG} || true
                        docker rmi ${DOCKER_HUB_REPO}:${env.CURRENT_BRANCH}-latest || true
                        if [ "${env.CURRENT_BRANCH}" = "main" ]; then
                            docker rmi ${DOCKER_HUB_REPO}:latest || true
                        fi
                    """
                }
            }
        }
    }

    post {
		always {
			echo 'Pipeline execution completed'
            cleanWs()
        }

        success {
			echo "✅ Pipeline completed successfully!"
            echo "🐳 Docker image: ${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG}"
            script {
				if (env.CURRENT_BRANCH == 'main' || env.CURRENT_BRANCH == 'develop') {
					echo "🚀 Image pushed to Docker Hub successfully!"
                }
            }
        }

        failure {
			echo "❌ Pipeline failed!"
            sh '''
                docker rmi ${DOCKER_HUB_REPO}:${DOCKER_IMAGE_TAG} || true
                docker image prune -f || true
            '''
        }

        unstable {
			echo "⚠️ Pipeline completed with warnings"
        }
    }
}
