pipeline {
    agent any
    stages {
        stage('test') {
            steps {
                sh 'lein do clean, test'
            }
        }
        stage('build') {
            steps {
                sh './bin/cibuild.sh create-uberjar'
            }
        }
    }
}
