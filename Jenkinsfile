pipeline {
    agent { docker 'clojure' }
    stages {
        stage('build') {
            steps {
                sh 'lein do clean, uberjar'
            }
        }
    }
}