pipeline {
    agent any
    environment {
        CONFIG= 'dev-resources/local-test-config.edn'
    }
    stages {
        stage('test') {
            steps {
                sh './bin/cibuild.sh run-tests'
            }
        }
        stage('package') {
            steps {
                sh './bin/cibuild.sh create-uberjar'
            }
        }
    }
}
