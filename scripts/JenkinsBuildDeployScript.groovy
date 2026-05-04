pipeline {
    agent any

    environment {
        GIT_ORG = "ashishyete"
        // Docker settings
        PORT = "9000"
    }

    stages {

        stage('Checkout Code') {
            steps {
                echo "======================================"
                echo "Repo   : ${params.REPO_NAME}"
                echo "Branch : ${params.BRANCH_NAME}"
                echo "======================================"

                git branch: "${params.BRANCH_NAME}",
                        url: "https://github.com/${env.GIT_ORG}/${params.REPO_NAME}.git"
            }
        }

        stage('Build Spring Boot App') {
            steps {
                echo "Building Spring Boot application using Maven..."

                sh "/usr/bin/mvn clean package -DskipTests"
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_NAME = "${params.REPO_NAME}:${params.BRANCH_NAME}-${BUILD_NUMBER}"
                }

                echo "Building Docker image: ${IMAGE_NAME}"

                sh """
                    docker build -t ${IMAGE_NAME} .
                """
            }
        }

        stage('Stop Old Container') {
            steps {
                script {
                    env.CONTAINER_NAME = "${params.REPO_NAME}-container"
                }

                echo "Stopping old container if running..."

                sh """
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true
                """
            }
        }

        stage('Run New Container') {
            steps {
                echo "Deploying new container..."

                sh """
                    docker run -d \
                    --name ${CONTAINER_NAME} \
                    -p ${PORT}:9000 \
                    ${IMAGE_NAME}
                """
            }
        }
    }

    post {
        success {
            echo "======================================"
            echo "Deployment SUCCESS"
            echo "App: ${params.REPO_NAME}"
            echo "Branch: ${params.BRANCH_NAME}"
            echo "Image: ${IMAGE_NAME}"
            echo "======================================"
        }

        failure {
            echo "======================================"
            echo "Deployment FAILED"
            echo "Check logs above"
            echo "======================================"
        }

        always {
            echo "Pipeline execution completed"
        }
    }
}