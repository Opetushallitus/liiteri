pipeline {
    agent any
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
