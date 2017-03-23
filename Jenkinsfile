pipeline {
    agent { docker 'clojure' }
    stages {
        stage('build') {
            steps {
                sh 'lein uberjar'
            }
        }
    }
}