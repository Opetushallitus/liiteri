pipeline {
    agent { docker 'clojure' }
    stages {
        stage('test') {
            steps {
                sh 'lein do clean, test'
            }
        }
        stage('build') {
            steps {
                sh 'lein do clean, uberjar'
            }
        }
    }
}