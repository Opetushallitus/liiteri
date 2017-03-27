pipeline {
    agent any
    stages {
        stage('test') {
            steps {
                sh 'lein do clean, test'
            }
        }
        stage('package') {
            steps {
                sh './bin/cibuild.sh create-uberjar'
            }
        }
    }
}
