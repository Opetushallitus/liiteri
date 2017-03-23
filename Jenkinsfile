pipeline {
    agent { docker 'lein' }
    stages {
        stage('build') {
            steps {
                sh 'lein uberjar'
            }
        }
    }
}